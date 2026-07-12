package com.elgi.creditsimulator.remote;

import com.elgi.creditsimulator.exception.InvalidInputException;
import com.elgi.creditsimulator.exception.RemoteServiceException;
import com.elgi.creditsimulator.factory.VehicleFactory;
import com.elgi.creditsimulator.json.JsonObject;
import com.elgi.creditsimulator.json.JsonParser;
import com.elgi.creditsimulator.model.LoanRequest;
import com.elgi.creditsimulator.model.Vehicle;

import java.math.BigDecimal;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Objects;

public class LoanApiClient {

    public static final String SPEC_URL = "https://run.mocky.io/v3/9108b1da-beec-409e-ae14-e212003666c";

    /** Overrides the endpoint without a rebuild. */
    public static final String URL_PROPERTY = "creditsimulator.api.url";

    private final HttpFetcher fetcher;
    private final URI endpoint;

    public LoanApiClient(HttpFetcher fetcher, URI endpoint) {
        this.fetcher = Objects.requireNonNull(fetcher, "fetcher");
        this.endpoint = Objects.requireNonNull(endpoint, "endpoint");
    }

    /** The shipping configuration: the real network, and the endpoint from the system property. */
    public static LoanApiClient createDefault() {
        return new LoanApiClient(new JdkHttpFetcher(), configuredEndpoint());
    }

    /** The system property if set, otherwise the URL the spec printed. */
    public static URI configuredEndpoint() {
        String configured = System.getProperty(URL_PROPERTY, SPEC_URL);
        try {
            return new URI(configured);
        } catch (URISyntaxException cause) {
            throw new RemoteServiceException(
                    "The configured service URL is not a valid URI: " + configured, cause);
        }
    }

    public URI endpoint() {
        return endpoint;
    }

    public LoanRequest loadExistingCalculation() {
        String body = fetcher.get(endpoint);
        JsonObject payload = JsonParser.parseObject(body);

        return toLoanRequest(payload);
    }

    static LoanRequest toLoanRequest(JsonObject payload) {
        try {
            Vehicle vehicle = VehicleFactory.create(
                    payload.getString("vehicleType"),
                    payload.getString("vehicleCondition"),
                    String.valueOf(payload.getInt("vehicleYear")));

            BigDecimal totalLoanAmount = payload.getBigDecimal("totalLoanAmount");
            BigDecimal downPayment = payload.getBigDecimal("downPayment");
            int loanTenure = payload.getInt("loanTenure");

            return new LoanRequest(vehicle, totalLoanAmount, downPayment, loanTenure);

        } catch (InvalidInputException cause) {
            throw new RemoteServiceException(
                    "The service returned a calculation this application cannot read: "
                            + cause.getMessage(), cause);
        }
    }

}
