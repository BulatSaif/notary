package notary

/**
 * Class represents commands that [Notary] can send to [sideChain.iroha.consumer.IrohaConsumer]
 */
sealed class IrohaCommand {

    /**
     * Class represents addAssetQuantity Iroha command
     * @param accountId account id to add asset quantity to
     * @param assetId asset id to add value to
     * @param amount is a string representation of amount to add
     */
    data class CommandAddAssetQuantity(
        val accountId: String,
        val assetId: String,
        val amount: String
    ) : IrohaCommand()

    /**
     * Class represents setAccountDetail Iroha command
     * @param accountId account id to add detail to
     * @param key detail key
     * @param value detail value
     */
    data class CommandSetAccountDetail(
        val accountId: String,
        val key: String,
        val value: String
    ) : IrohaCommand()

    /**
     * Class represents createAsset Iroha command
     * @param assetName - asset name to create
     * @param domainId - domain id to create asset in
     * @param precision - asset precision
     */
    data class CommandCreateAsset(
        val assetName: String,
        val domainId: String,
        val precision: Short
    ) : IrohaCommand()

    /**
     * Class represents transferAsset Iroha command
     * @param srcAccountId - source account id
     * @param destAccountId - destination account id
     * @param assetId - asset id
     * @param description - description message which user can set
     * @param amount - amount of asset to transfer
     */
    data class CommandTransferAsset(
        val srcAccountId: String,
        val destAccountId: String,
        val assetId: String,
        val description: String,
        val amount: String
    ) : IrohaCommand()

    /**
     * Class represents addSignatory Iroha command
     * @param accountId id of signatory's account
     * @param publicKey public key of signatory
     */
    data class CommandAddSignatory(
        val accountId: String,
        val publicKey: String
    ) : IrohaCommand()
}
