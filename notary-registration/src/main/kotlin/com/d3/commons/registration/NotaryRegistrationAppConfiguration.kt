package com.d3.commons.registration

import com.d3.commons.config.loadConfigs
import com.d3.commons.model.IrohaCredential
import com.d3.commons.sidechain.iroha.consumer.IrohaConsumerImpl
import com.d3.commons.sidechain.iroha.util.ModelUtil
import jp.co.soramitsu.iroha.java.IrohaAPI
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

val registrationConfig =
    loadConfigs("registration", NotaryRegistrationConfig::class.java, "/registration.properties").get()

/**
 * Spring configuration for Notary Registration Service
 */
@Configuration
class NotaryRegistrationAppConfiguration {

    /** Registartion service credentials */
    private val registrationCredential = ModelUtil.loadKeypair(
        registrationConfig.registrationCredential.pubkeyPath,
        registrationConfig.registrationCredential.privkeyPath
    ).fold(
        { keypair ->
            IrohaCredential(registrationConfig.registrationCredential.accountId, keypair)
        },
        { ex -> throw ex }
    )

    /** Iroha network connection */
    @Bean
    fun irohaAPI() = IrohaAPI(registrationConfig.iroha.hostname, registrationConfig.iroha.port)

    @Bean
    fun irohaConsumer() = IrohaConsumerImpl(
        registrationCredential, irohaAPI()
    )

    /** Configurations for Notary Registration Service */
    @Bean
    fun registrationConfig() = registrationConfig

    @Bean
    fun clientStorageAccount() = registrationConfig().clientStorageAccount

    @Bean
    fun brvsAccount() = registrationConfig().brvsAccount

    @Bean
    fun primaryKeyPair() =
        ModelUtil.loadKeypair(registrationConfig.primaryPubkeyPath, registrationConfig.primaryPrivkeyPath).get()
}
