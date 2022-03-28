package ws.palladian.retrieval;

import org.apache.commons.lang3.Validate;

public final class OAuthParams {

    /** OAuth 1 signature methods.
    * @since 2.0 */
    public static enum SignatureMethod {
        HMAC_SHA1("HMAC-SHA1", "HmacSHA1"),
        HMAC_SHA256("HMAC-SHA256", "HmacSHA256"),
        HMAC_SHA512("HMAC-SHA512", "HmacSHA512"),
        // these are currently not implemented
        // RSA_SHA1,
        // RSA_SHA256,
        // RSA_SHA512,
        // PLAINTEXT
        ;

        /** OAuth spec. identifier of the signature method. */
        public final String methodValue;
        /** javax.crypto algorithm identifier */
        public final String cryptoAlgorithm;

        private SignatureMethod(String methodValue, String cryptoAlgorithm) {
            this.methodValue = methodValue;
            this.cryptoAlgorithm = cryptoAlgorithm;
        }

        @Override
        public String toString() {
            return methodValue;
        }
    }

    private final String consumerKey;
    private final String consumerSecret;
    private final String accessToken;
    private final String accessTokenSecret;
    private final SignatureMethod signatureMethod;

    /**
     * Create a {@link OAuthParams} instance.
     * 
     * @param consumerKey The consumer key, not <code>null</code>.
     * @param consumerSecret The consumer secret, not <code>null</code>.
     * @param accessToken Optional access token.
     * @param accessTokenSecret Optional access token secret.
     * @param signatureMethod The signature method.
     * @since 2.0
     */
    public OAuthParams(String consumerKey, String consumerSecret, String accessToken, String accessTokenSecret, SignatureMethod signatureMethod) {
        Validate.notEmpty(consumerKey, "consumerKey must not be empty");
        Validate.notEmpty(consumerSecret, "consumerSecret must not be empty");

        this.consumerKey = consumerKey;
        this.consumerSecret = consumerSecret;
        this.accessToken = accessToken;
        this.accessTokenSecret = accessTokenSecret;
        this.signatureMethod = signatureMethod;
    }

    /**
     * Create a {@link OAuthParams} instance using HMAC-SHA1.
     * 
     * @param consumerKey The consumer key, not <code>null</code>.
     * @param consumerSecret The consumer secret, not <code>null</code>.
     * @param accessToken Optional access token.
     * @param accessTokenSecret Optional access token secret.
     */
    public OAuthParams(String consumerKey, String consumerSecret, String accessToken, String accessTokenSecret) {
        this(consumerKey, consumerSecret, accessToken, accessTokenSecret, SignatureMethod.HMAC_SHA1);
    }

    public String getConsumerKey() {
        return consumerKey;
    }

    public String getConsumerSecret() {
        return consumerSecret;
    }

    public String getAccessToken() {
        return accessToken;
    }

    public String getAccessTokenSecret() {
        return accessTokenSecret;
    }

    /** @return The signature method.
     * @since 2.0 
     */
    public SignatureMethod getSignatureMethod() {
        return signatureMethod;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("OAuthParams [consumerKey=");
        builder.append(getConsumerKey());
        builder.append(", consumerSecret=");
        builder.append(getConsumerSecret());
        if (getAccessToken() != null) {
            builder.append(", accessToken=");
            builder.append(getAccessToken());
        }
        if (getAccessTokenSecret() != null) {
            builder.append(", accessTokenSecret=");
            builder.append(getAccessTokenSecret());
        }
        builder.append(", signatureMethod=");
        builder.append(getSignatureMethod());
        builder.append("]");
        return builder.toString();
    }

}