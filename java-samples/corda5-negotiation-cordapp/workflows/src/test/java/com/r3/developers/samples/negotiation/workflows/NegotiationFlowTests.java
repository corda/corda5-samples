package com.r3.developers.samples.negotiation.workflows;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.r3.developers.samples.negotiation.workflows.accept.AcceptFlowArgs;
import com.r3.developers.samples.negotiation.workflows.accept.AcceptFlowRequest;
import com.r3.developers.samples.negotiation.workflows.modify.ModifyFlowArgs;
import com.r3.developers.samples.negotiation.workflows.modify.ModifyFlowRequest;
import com.r3.developers.samples.negotiation.workflows.propose.ProposalFlowArgs;
import com.r3.developers.samples.negotiation.workflows.propose.ProposalFlowRequest;
import net.corda.testing.driver.AllTestsDriver;
import net.corda.testing.driver.DriverNodes;
import net.corda.v5.base.types.MemberX500Name;
import net.corda.v5.ledger.utxo.UtxoLedgerService;
import net.corda.v5.application.flows.CordaInject;
import net.corda.virtualnode.VirtualNodeInfo;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertTrue;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class NegotiationFlowTests {
    private static final Logger logger = LoggerFactory.getLogger(NegotiationFlowTests.class);
    private static final MemberX500Name alice = MemberX500Name.parse("CN=Alice, OU=Application, O=R3, L=London, C=GB");
    private static final MemberX500Name bob = MemberX500Name.parse("CN=Bob, OU=Application, O=R3, L=London, C=GB");
    private static final MemberX500Name charles = MemberX500Name.parse("CN=Charles, OU=Application, O=R3, L=London, C=GB");
    private Map<MemberX500Name, VirtualNodeInfo> virtualNodes;
    @CordaInject
    UtxoLedgerService utxoLedgerService;
    private static final ObjectMapper jsonMapper;

    static {
        jsonMapper = new ObjectMapper();
        SimpleModule module = new SimpleModule();
        module.addSerializer(MemberX500Name.class, new MemberX500NameSerializer());
        module.addDeserializer(MemberX500Name.class, new MemberX500NameDeserializer());
        jsonMapper.registerModule(module);
    }

    // Create the driver with Alice and Bob as participants, and Charlie as the notary
    // This is to be created for all tests
    @RegisterExtension
    private final AllTestsDriver driver =
            new DriverNodes(alice, bob, charles).withNotary(charles, 1).forAllTests();

    @BeforeAll
    void setup() {
        // Start the virtual nodes for Alice and Bob
        virtualNodes = driver.let(dsl -> {
            dsl.startNodes(Set.of(alice, bob));
            return dsl.nodesFor("workflows");
        });

        assertThat(virtualNodes).withFailMessage("Failed to populate vNodes").isNotEmpty();
    }

    @Test
    void test_that_ModifyProposalWithInvalidExistingProposalId_throws_exception() throws JsonProcessingException {
        MemberX500Name responder = bob;
        UUID fakeProposalId = UUID.randomUUID();
        // Create accept proposal flow using the fake proposal ID
        String fakeModifyProposalFlowArgs = jsonMapper.writeValueAsString(
                new ModifyFlowArgs(fakeProposalId, 30));

        assertThatThrownBy(() -> {
            // Execute the modify proposal flow with fake modify proposal arguments and Bob as the issuer
            driver.let(dsl ->
                    dsl.runFlow(virtualNodes.get(responder),
                            ModifyFlowRequest.class,
                            () -> fakeModifyProposalFlowArgs)
            );
        }).isInstanceOf(net.corda.testing.driver.node.FlowErrorException.class)
                .hasMessageContaining("Multiple or zero Proposal states not found wth id:");
    }

    @Test
    void test_that_ModifyProposalWithValidExistingProposalId_returns_newProposalId() throws JsonProcessingException {
        MemberX500Name issuer = alice;
        MemberX500Name responder = bob;

        // First issue a proposal
        // Create proposal flow arguments with Bob as the responder
        String proposalFlowArgs = jsonMapper.writeValueAsString(
                new ProposalFlowArgs(20, responder, false));

        // Execute the proposal flow using the proposal arguments created above with Alice as the issuer
        String proposalId = driver.let(dsl ->
                dsl.runFlow(virtualNodes.get(issuer),
                        ProposalFlowRequest.class,
                        () -> proposalFlowArgs)
        );

        // Secondly, modify the newly created proposal
        // Create modify proposal flow arguments
        String modifyProposalFlowArgs = jsonMapper.writeValueAsString(
                new ModifyFlowArgs(UUID.fromString(proposalId), 30));

        // Execute the modify proposal flow with Bob as the issuer
        String newProposalId = driver.let(dsl ->
                dsl.runFlow(virtualNodes.get(responder),
                        ModifyFlowRequest.class,
                        () -> modifyProposalFlowArgs)
        );

        // Assert that a trade was created (tradeId should be available)
        assertThat(UUID.fromString(newProposalId)).isInstanceOf(UUID.class);
        assertTrue(proposalId != newProposalId);
    }

    @Test
    void test_that_IssueByAliceModifyByBobAcceptByBob_throws_exception() throws JsonProcessingException {
        MemberX500Name issuer = alice;
        MemberX500Name responder = bob;

        // First Alice issues a proposal
        // Create proposal flow arguments with Bob as the responder
        String proposalFlowArgs = jsonMapper.writeValueAsString(
                new ProposalFlowArgs(20, responder, false));

        // Execute the proposal flow using the proposal arguments created above with Alice as the issuer
        String proposalId = driver.let(dsl ->
                dsl.runFlow(virtualNodes.get(issuer),
                        ProposalFlowRequest.class,
                        () -> proposalFlowArgs)
        );

        // Secondly, Bob modifies the proposal created by Alice
        // Create modify proposal flow arguments
        String modifyProposalFlowArgs = jsonMapper.writeValueAsString(
                new ModifyFlowArgs(UUID.fromString(proposalId), 30));

        // Execute the modify proposal flow with Bob as the issuer
        String newProposalId = driver.let(dsl ->
                dsl.runFlow(virtualNodes.get(responder),
                        ModifyFlowRequest.class,
                        () -> modifyProposalFlowArgs)
        );

        // Thirdly, Bob accepts the proposal that he modified, Alice did not get a say
        // Create modify proposal flow arguments
        String acceptProposalFlowArgs = jsonMapper.writeValueAsString(
                new AcceptFlowArgs(UUID.fromString(newProposalId), responder));

        assertThatThrownBy(() -> {
            // Execute the accept proposal flow with Bob as the issuer or proposer
            driver.let(dsl ->
                    dsl.runFlow(virtualNodes.get(responder),
                            AcceptFlowRequest.class,
                            () -> acceptProposalFlowArgs)
            );
        }).isInstanceOf(net.corda.testing.driver.node.FlowErrorException.class)
                .hasMessageContaining("The proposer cannot accept their own proposal");
    }

    @Test
    void test_that_IssueByAliceModifyByBobAcceptByAlice_returns_tradeId() throws JsonProcessingException {
        MemberX500Name issuer = alice;
        MemberX500Name responder = bob;

        // First Alice issues a proposal
        // Create proposal flow arguments with Bob as the responder
        String proposalFlowArgs = jsonMapper.writeValueAsString(
                new ProposalFlowArgs(20, responder, false));

        // Execute the proposal flow using the proposal arguments created above with Alice as the issuer
        String proposalId = driver.let(dsl ->
                dsl.runFlow(virtualNodes.get(issuer),
                        ProposalFlowRequest.class,
                        () -> proposalFlowArgs)
        );

        // Secondly, Bob modifies the proposal created by Alice
        // Create modify proposal flow arguments
        String modifyProposalFlowArgs = jsonMapper.writeValueAsString(
                new ModifyFlowArgs(UUID.fromString(proposalId), 30));

        // Execute the modify proposal flow with Bob as the issuer
        String newProposalId = driver.let(dsl ->
                dsl.runFlow(virtualNodes.get(responder),
                        ModifyFlowRequest.class,
                        () -> modifyProposalFlowArgs)
        );

        // Thirdly, Bob accepts the proposal that he modified, Alice did not get a say
        // Create modify proposal flow arguments
        String acceptProposalFlowArgs = jsonMapper.writeValueAsString(
                new AcceptFlowArgs(UUID.fromString(newProposalId), issuer));

            // Execute the accept proposal flow with Bob as the issuer or proposer
            String tradeId = driver.let(dsl ->
                    dsl.runFlow(virtualNodes.get(issuer),
                            AcceptFlowRequest.class,
                            () -> acceptProposalFlowArgs)
            );

        // Assert that a trade was created (tradeId should be available)
        assertThat(UUID.fromString(tradeId)).isInstanceOf(UUID.class);
    }

    @Test
    void test_that_AcceptProposalWithInvalidProposalId_throws_exception() throws JsonProcessingException {
        MemberX500Name responder = bob;
        UUID fakeProposalId = UUID.randomUUID();
        // Create accept proposal flow using the fake proposal ID
        String fakeAcceptProposalFlowArgs = jsonMapper.writeValueAsString(
                new AcceptFlowArgs(fakeProposalId, responder));

        assertThatThrownBy(() -> {
            // Execute the accept proposal flow with fake proposal arguments and Bob as the issuer
             driver.let(dsl ->
                    dsl.runFlow(virtualNodes.get(responder),
                            AcceptFlowRequest.class,
                            () -> fakeAcceptProposalFlowArgs)
            );
        }).isInstanceOf(net.corda.testing.driver.node.FlowErrorException.class)
                .hasMessageContaining("Multiple or zero Proposal states not found wth id:");
    }

    @Test
    void test_that_AcceptProposalWithValidProposalId_returns_tradeId() throws JsonProcessingException {
        MemberX500Name issuer = alice;
        MemberX500Name responder = bob;

        // First issue a proposal
        // Create proposal flow arguments with Bob as the responder
        String proposalFlowArgs = jsonMapper.writeValueAsString(
                new ProposalFlowArgs(20, responder, false));

        // Execute the proposal flow using the proposal arguments created above with Alice as the issuer
        String proposalId = driver.let(dsl ->
                dsl.runFlow(virtualNodes.get(issuer),
                        ProposalFlowRequest.class,
                        () -> proposalFlowArgs)
        );

        // Secondly, accept the newly created proposal
        // Create accept proposal flow arguments
        String acceptProposalFlowArgs = jsonMapper.writeValueAsString(
                new AcceptFlowArgs(UUID.fromString(proposalId), responder));

        // Execute the accept proposal flow with Bob as the issuer
        String tradeId = driver.let(dsl ->
                dsl.runFlow(virtualNodes.get(responder),
                        AcceptFlowRequest.class,
                        () -> acceptProposalFlowArgs)
        );

        // Assert that a trade was created (tradeId should be available)
        assertThat(UUID.fromString(tradeId)).isInstanceOf(UUID.class);
    }

    @Test
    void test_that_IssueProposalWithIssuerAsBuyer_returns_proposalId() throws JsonProcessingException {
        MemberX500Name issuer = alice;
        MemberX500Name responder = bob;

        // Create proposal flow arguments with Bob as the responder
        String proposalFlowArgs = jsonMapper.writeValueAsString(
                new ProposalFlowArgs(20, responder, false));

        // Execute the proposal flow with the proposal arguments created above and Alice as the issuer
        String result = driver.let(dsl ->
                dsl.runFlow(virtualNodes.get(issuer),
                        ProposalFlowRequest.class,
                        () -> proposalFlowArgs)
        );
        // Check that a proposal was created (proposalId should be available)
        assertThat(UUID.fromString(result)).isInstanceOf(UUID.class);
    }

    @Test
    void test_that_IssueProposalWithIssuerNotAsBuyer_throws_exception() throws JsonProcessingException {
        MemberX500Name issuer = alice;
        MemberX500Name responder = bob;

        // Create proposal flow arguments with Bob as the responder and the buyer
        // (therefore, Alice who is the issuer is not the buyer, hence violating the smart contract)
        String proposalFlowArgs = jsonMapper.writeValueAsString(
                new ProposalFlowArgs(20, responder, true));

        assertThatThrownBy(() -> {
            // Run the proposal flow using the proposal arguments created above
            String result = driver.let(dsl ->
                    dsl.runFlow(virtualNodes.get(issuer),
                            ProposalFlowRequest.class,
                            () -> proposalFlowArgs));
        }).isInstanceOf(net.corda.testing.driver.node.FlowErrorException.class)
                .hasMessageContaining("The buyer are the proposer");
    }
}
