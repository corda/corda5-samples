## Important Note

corda services like oracles are an outdated feature. please see blog: https://corda.net/blog/corda-services-in-corda-5/



## Challenges that came along the way
- oracles removed https://corda.net/blog/corda-services-in-corda-5/
- primitive type flow session message are illegal https://r3holdco.slack.com/archives/C039F5SAFNH/p1683800733619779


```kotlin
        val notary = memberLookup.lookup(notaryName) ?: throw IllegalArgumentException("Requested oracle '$oracleName' not found on network.")
        val notaryInfo = notaryLookup.notaryServices.first()
        log.info("[DEBUG1.1] what's the difference between notary($${notary}) and notaryInfo(${notaryInfo})")
```
gave
```text
2023-07-28 14:35:08.900 [single-threaded-scheduled-executor-1] INFO  com.r3.developers.samples.primenumber.flows.CreatePrime {corda.client.id=create-prime-1, flow.id=7deba0c6-db55-49e9-adfe-d4adc81f9246, vnode.id=7DC4FA98B4CD} - [DEBUG1.1] what's the difference between notary($net.corda.membership.lib.impl.MemberInfoImpl@3559b6ee) and notaryInfo(NotaryInfoImpl(name=CN=NotaryService, OU=Test Dept, O=R3, L=London, C=GB, protocol=com.r3.corda.notary.plugin.nonvalidating, protocolVersions=[1], publicKey=EC Public Key [84:95:2e:2d:cf:0f:71:3d:ff:56:42:1e:ca:70:30:f6:dc:d2:7e:70]
```
