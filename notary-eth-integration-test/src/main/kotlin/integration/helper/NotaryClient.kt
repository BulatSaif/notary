package integration.helper

import com.d3.commons.config.EthereumConfig
import com.d3.commons.config.EthereumPasswords
import com.d3.commons.model.IrohaCredential
import mu.KLogging
import com.d3.eth.sidechain.util.DeployHelper
import com.d3.commons.sidechain.iroha.util.ModelUtil
import com.d3.commons.util.getRandomString
import com.d3.commons.util.toHexString
import java.math.BigInteger

/**
 * Notary client class.
 * Represents client, his intentions and actions.
 */
class NotaryClient(
    val integrationHelper: EthIntegrationHelperUtil,
    ethConfig: EthereumConfig,
    ethPasswordConfig: EthereumPasswords,
    val name: String = "client_${String.getRandomString(6)}"
) {
    private val irohaCredential = IrohaCredential("$name@notary", ModelUtil.generateKeypair())

    private val etherHelper = DeployHelper(ethConfig, ethPasswordConfig)

    /** Notary Iroha account id */
    private val notaryAccountId = integrationHelper.accountHelper.notaryAccount.accountId

    /** Client Iroha account id */
    val accountId = irohaCredential.accountId

    /** Client ethereum wallet address outside notary */
    val ethAddress = etherHelper.credentials.address

    /** Client whitelist (where to withdraw) */
    val whitelist: List<String> = listOf(ethAddress)

    /** Client relay */
    var relay: String? = null

    init {
        logger.info { "Created client $accountId with eth address $ethAddress." }
    }

    /**
     * Send HTTP POST request to registration service to register user
     */
    fun signUp(): khttp.responses.Response {
        val response = integrationHelper.sendRegistrationRequest(
            name,
            whitelist.toString(),
            irohaCredential.keyPair.public.toHexString(),
            integrationHelper.ethRegistrationConfig.port
        )
        relay = response.text

        return response
    }

    fun deposit(amount: BigInteger) {
        if (relay != null)
            etherHelper.sendEthereum(amount, relay!!)
        else
            throw Exception("Relay not registered.")
    }

    /**
     * Send intension to withdraw asset.
     * Transfers iroha asset to notary account.
     */
    fun withdraw(amount: String) {
        logger.info { "Client $name wants to withdraw $amount ether" }
        integrationHelper.transferAssetIrohaFromClient(
            irohaCredential.accountId,
            irohaCredential.keyPair,
            irohaCredential.accountId,
            notaryAccountId,
            "ether#ethereum",
            ethAddress,
            amount
        )
    }

    /**
     * Transfer asset to another client.
     */
    fun transfer(amount: String, to: String) {
        logger.info { "Client $name wants to transfer $amount ether to $to" }
        integrationHelper.transferAssetIrohaFromClient(
            irohaCredential.accountId,
            irohaCredential.keyPair,
            irohaCredential.accountId,
            to,
            "ether#ethereum",
            "",
            amount
        )
    }

    /**
     * Get ethereum wallet balance of client wallet.
     */
    fun getEthBalance(): BigInteger {
        return integrationHelper.getEthBalance(ethAddress)
    }

    /**
     * Get iroha client balance.
     */
    fun getIrohaBalance(): String {
        return integrationHelper.getIrohaAccountBalance(irohaCredential.accountId, "ether#ethereum")
    }

    /**
     * Logger
     */
    companion object : KLogging()
}
