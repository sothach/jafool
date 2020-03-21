package org.anized.jafool;

import org.apache.camel.Converter;
import org.apache.camel.Exchange;
import org.apache.camel.TypeConverters;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.jackson.JacksonDataFormat;

import java.time.ZonedDateTime;

import static com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES;
import static org.apache.camel.http.common.HttpMethods.GET;

public class CamelRoute extends RouteBuilder {
    private static final JacksonDataFormat jsonFormat = new JacksonDataFormat();
    static {
        jsonFormat.disableFeature(FAIL_ON_UNKNOWN_PROPERTIES);  // should only map fields whose names match
        jsonFormat.setUnmarshalType(DateTimeReport.class);      // (COBOL's "MOVE CORRESPONDING" lives on!)
    }

    private String worldClockUrl;

    /** build a route to call the worldclock endpoint and process the response
     * @param worldClockUrl
     *   worldclock endpoint url, e.g., "http://worldclockapi.com/api/json/${body}/now";
     *   the placeholder '${body}' will be replaced with the current value of
     *   {@code exchange.getIn().getBody()} at the point in the route this value is used */
    public CamelRoute(final String worldClockUrl) {
        this.worldClockUrl = worldClockUrl;
    }

    @Override
    public void configure() {
        getContext().getTypeConverterRegistry()
                .addTypeConverters(new EventTypeConverters());  // add convert

        from("direct:worldclock").routeId("clock")              // give the route an id, to help logging
                .setHeader(Exchange.HTTP_METHOD).constant(GET)  // set-up for a GET call
                .to("log:org.anized?level=DEBUG&showAll=true")  // log the exchange content at each stage
                .toD(worldClockUrl)                             // send to the dynamic endpoint (adds body into url)
                .to("log:org.anized?level=DEBUG&showAll=true")
                .unmarshal(jsonFormat)                          // parse the Json response returned into domain object
                .to("log:org.anized?level=DEBUG&showAll=true")
                .convertBodyTo(ZonedDateTime.class);            // transform response to required return type
    }

    public static class EventTypeConverters implements TypeConverters {
        /** Map a DateTimeReport object to a {@code ZonedDateTime} object
          * @param report domain object returned from the world clock service
          * @return ZonedDateTime set from the {@code currentDateTime} in the {@code report} */
        @Converter
        public ZonedDateTime toZonedDateTime(final DateTimeReport report) {
            return ZonedDateTime.parse(report.currentDateTime);
        }
    }
}
