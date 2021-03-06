package com.d3.eth.registration.relay

import com.d3.commons.config.EthereumPasswords
import com.d3.commons.model.IrohaCredential
import com.d3.commons.sidechain.iroha.consumer.IrohaConsumerImpl
import com.d3.commons.sidechain.iroha.util.ModelUtil
import com.d3.eth.provider.EthFreeRelayProvider
import com.d3.eth.sidechain.util.DeployHelper
import com.github.kittinunf.result.Result
import com.github.kittinunf.result.failure
import com.github.kittinunf.result.flatMap
import jp.co.soramitsu.iroha.java.IrohaAPI
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import mu.KLogging
import java.io.File

/**
 * Class is responsible for relay addresses registration.
 * Deploys relay smart contracts in Ethereum network and records it in Iroha.
 */
class RelayRegistration(
    private val freeRelayProvider: EthFreeRelayProvider,
    private val relayRegistrationConfig: RelayRegistrationConfig,
    relayCredential: IrohaCredential,
    irohaAPI: IrohaAPI,
    relayRegistrationEthereumPasswords: EthereumPasswords
) {
    /** Ethereum endpoint */
    private val deployHelper =
        DeployHelper(relayRegistrationConfig.ethereum, relayRegistrationEthereumPasswords)

    /** Iroha endpoint */
    private val irohaConsumer = IrohaConsumerImpl(relayCredential, irohaAPI)

    private val notaryIrohaAccount = relayRegistrationConfig.notaryIrohaAccount

    /**
     * Registers relay in Iroha.
     * @param relayAddress - relay address to record into Iroha
     * @return Result with string representation of hash or possible failure
     */
    fun registerRelayIroha(relayAddress: String): Result<String, Exception> {
        return ModelUtil.setAccountDetail(irohaConsumer, notaryIrohaAccount, relayAddress, "free")
    }

    fun deploy(
        relaysToDeploy: Int,
        ethRelayImplementationAddress: String,
        ethMasterWallet: String
    ): Result<Unit, Exception> {
        return Result.of {
            logger.info { "Deploy $relaysToDeploy ethereum relays" }

            (1..relaysToDeploy).forEach { _ ->
                val relayWallet =
                    deployHelper.deployUpgradableRelaySmartContract(ethRelayImplementationAddress, ethMasterWallet)
                        .contractAddress
                registerRelayIroha(relayWallet).fold(
                    { logger.info("Relay $relayWallet was deployed") },
                    { ex -> logger.error("Cannot deploy relay $relayWallet", ex) })
            }
        }
    }

    /**
     * Run a job that every replenishmentPeriod checks that number from config free relays are present. In case of
     * lack of free relays deploys lacking amount.
     */
    fun runRelayReplenishment(): Result<Unit, Exception> {
        logger.info { "Run relay replenishment" }

        return Result.of {
            while (true) {
                logger.info { "Relay replenishment triggered" }

                freeRelayProvider.getRelays().flatMap { relays ->
                    logger.info { "Free relays: ${relays.size}" }
                    val toDeploy = relayRegistrationConfig.number - relays.size
                    deploy(
                        toDeploy,
                        relayRegistrationConfig.ethRelayImplementationAddress,
                        relayRegistrationConfig.ethMasterWallet
                    )
                }.failure { throw it }

                runBlocking { delay(relayRegistrationConfig.replenishmentPeriod * 1000) }
            }
        }
    }

    fun import(filename: String): Result<Unit, Exception> {
        return Result.of {
            getRelaysFromFile(filename).forEach { relay ->
                registerRelayIroha(relay).fold(
                    { logger.info("Relay $relay was imported") },
                    { ex -> logger.error("Cannot import relay $relay", ex) })
            }
        }
    }

    private fun getRelaysFromFile(filename: String): List<String> {
        return File(filename).readLines()
    }

    /**
     * Logger
     */
    companion object : KLogging()

}
