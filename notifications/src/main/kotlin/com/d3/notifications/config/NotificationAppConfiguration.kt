package com.d3.notifications.config

import com.d3.notifications.provider.D3ClientProvider
import com.d3.notifications.push.PushServiceFactory
import com.d3.notifications.smtp.SMTPServiceImpl
import com.d3.commons.config.getConfigFolder
import com.d3.commons.config.loadRawConfigs
import io.grpc.ManagedChannelBuilder
import jp.co.soramitsu.iroha.java.IrohaAPI
import jp.co.soramitsu.iroha.java.QueryAPI
import com.d3.commons.model.IrohaCredential
import nl.martijndwars.webpush.PushService
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import com.d3.commons.sidechain.iroha.IrohaChainListener
import com.d3.commons.sidechain.iroha.util.ModelUtil
import java.io.File

val notificationsConfig = loadRawConfigs(
    "notifications", NotificationsConfig::class.java, getConfigFolder() + "/notifications.properties"
)

@Configuration
class NotificationAppConfiguration {

    private val notaryKeypair = ModelUtil.loadKeypair(
        notificationsConfig.notaryCredential.pubkeyPath,
        notificationsConfig.notaryCredential.privkeyPath
    ).fold({ keypair -> keypair }, { ex -> throw ex })

    private val notaryCredential = IrohaCredential(notificationsConfig.notaryCredential.accountId, notaryKeypair)

    private val pushAPIConfig = loadRawConfigs(
        "push",
        PushAPIConfig::class.java,
        getConfigFolder() + File.separator + notificationsConfig.pushApiConfigPath
    )

    @Bean
    fun irohaAPI(): IrohaAPI {
        val irohaAPI = IrohaAPI(notificationsConfig.iroha.hostname, notificationsConfig.iroha.port)
        irohaAPI.setChannelForStreamingQueryStub(
            ManagedChannelBuilder.forAddress(
                notificationsConfig.iroha.hostname, notificationsConfig.iroha.port
            ).directExecutor().usePlaintext().build()
        )
        return irohaAPI
    }

    @Bean
    fun notaryQueryAPI() = QueryAPI(irohaAPI(), notificationsConfig.notaryCredential.accountId, notaryKeypair)

    @Bean
    fun smtpService() =
        SMTPServiceImpl(
            loadRawConfigs(
                "smtp",
                SMTPConfig::class.java,
                getConfigFolder() + File.separator + notificationsConfig.smtpConfigPath
            )
        )

    @Bean
    fun d3ClientProvider() = D3ClientProvider(notaryQueryAPI())

    @Bean
    fun irohaChainListener() = IrohaChainListener(irohaAPI(), notaryCredential)

    @Bean
    fun pushServiceFactory() = object : PushServiceFactory {
        override fun create() =
            PushService(pushAPIConfig.vapidPubKeyBase64, pushAPIConfig.vapidPrivKeyBase64, "D3 notifications")
    }
}
