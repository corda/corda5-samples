package com.r3.developers.samples.referencestate.contracts;

import com.r3.developers.samples.referencestate.states.Member;
import com.r3.developers.samples.referencestate.states.SanctionableIOUState;
import com.r3.developers.samples.referencestate.states.SanctionList;
import net.corda.v5.base.exceptions.CordaRuntimeException;
import net.corda.v5.base.types.MemberX500Name;
import net.corda.v5.ledger.utxo.Command;
import net.corda.v5.ledger.utxo.Contract;
import net.corda.v5.ledger.utxo.transaction.UtxoLedgerTransaction;
import org.jetbrains.annotations.NotNull;

import java.security.PublicKey;
import java.util.List;
import java.util.stream.Collectors;

/**
 * A implementation of a basic smart contracts in Corda.
 *
 * This contracts enforces rules regarding the creation of a valid [SanctionableIOUState], which in turn encapsulates an [IOU].
 *
 * For a new [IOU] to be issued onto the ledger, a transaction is required which takes:
 * - Zero input states.
 * - One output states: the new [IOU].
 * - An Create() command with the public keys of both the lender and the borrower.
 *
 * All contracts must sub-class the [Contract] interface.
 */
public class SanctionableIOUContract implements Contract {

    public static class Create implements Command {
        private MemberX500Name sanctionAuthority;

        public Create(MemberX500Name sanctionAuthority) {
            this.sanctionAuthority = sanctionAuthority;
        }

        public MemberX500Name getsanctionAuthority() {
            return sanctionAuthority;
        }
    }

    static final String REQUIRES_CREATE_COMMAND = "The transaction requires the create command";
    static final String REQUIRES_SANCTIONED_ENTITY = "All transactions require a list of sanctioned entities";
    static final String REQUIRES_ZERO_INPUT = "No inputs should be consumed when issuing an IOU.";
    static final String REQUIRES_ONE_OUTPUT = "Only one output states should be produced.";
    static final String REQUIRES_ONE_IOU_OUTPUT =
            "Only one output states of type SanctionableIOUState should be produced.";
    static final String REQUIRE_DIFFERENT_LENDER_BORROWER = "The lender and the borrower cannot be the same entity.";
    static final String REQUIRE_ALL_SIGNATURE = "All of the participants must be signers.";
    static final String REQUIRE_POSITIVE_IOU_VALUE = "The IOU's value must be non-negative.";

    @Override
    public void verify(@NotNull UtxoLedgerTransaction transaction) {
        requireThat(transaction.getCommands(Create.class).size() == 1, REQUIRES_CREATE_COMMAND);
        Create command = transaction.getCommands(Create.class).get(0);

        requireThat(transaction.getReferenceStates(
                SanctionList.class).size() > 0, REQUIRES_SANCTIONED_ENTITY);

        SanctionList sanctionList = transaction.getReferenceStates(SanctionList.class).get(0);
        requireThat(
                sanctionList.getIssuer().getName().equals(command.getsanctionAuthority()),
                sanctionList.getIssuer().getName().getOrganization() + " is an invalid issuer of " +
                        "sanctions lists for this contracts."
        );

        requireThat(transaction.getInputContractStates().isEmpty(), REQUIRES_ZERO_INPUT);
        requireThat(transaction.getOutputContractStates().size() == 1, REQUIRES_ONE_OUTPUT);
        requireThat(transaction.getOutputStates(SanctionableIOUState.class).size() == 1, REQUIRES_ONE_IOU_OUTPUT);

        SanctionableIOUState out = transaction.getOutputStates(SanctionableIOUState.class).get(0);
        requireThat(!out.getLender().equals(out.getBorrower()), REQUIRE_DIFFERENT_LENDER_BORROWER);
        requireThat(transaction.getSignatories().containsAll(out.getParticipants()), REQUIRE_ALL_SIGNATURE);

        // IOU-specific constraints.
        requireThat(out.getValue()>0, REQUIRE_POSITIVE_IOU_VALUE);

        List<PublicKey> sanctionedPK = sanctionList.getBadPeople().stream().map(Member::getLedgerKey)
                .collect(Collectors.toList());
        requireThat(!sanctionedPK.contains(out.getLender().getLedgerKey()),
                "The lender " + out.getLender().getName() + " is a sanctioned entity");
        requireThat(!sanctionedPK.contains(out.getBorrower().getLedgerKey()),
                "The borrower " + out.getBorrower().getName() + " is a sanctioned entity");
    }

    private void requireThat(boolean asserted, String errorMessage) {
        if(!asserted) {
            throw new CordaRuntimeException("Failed requirement: " + errorMessage);
        }
    }
}
