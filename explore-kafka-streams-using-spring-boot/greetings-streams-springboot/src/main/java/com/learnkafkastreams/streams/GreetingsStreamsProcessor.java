package com.learnkafkastreams.streams;

import com.learnkafkastreams.domain.Greeting;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.kstream.Consumed;
import org.apache.kafka.streams.kstream.Printed;
import org.apache.kafka.streams.kstream.Produced;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.support.serializer.JsonSerde;
import org.springframework.stereotype.Component;

@Component
public class GreetingsStreamsProcessor {


    public static String GREETINGS ="greetings";
    public static String GREETINGS_OUTPUT ="greetings-output";

    @Autowired
    public void process(StreamsBuilder streamsBuilder){

        var greetingsStream = streamsBuilder
                .stream(GREETINGS,
                        Consumed.with(Serdes.String(),
                                //Serdes.String())
                                new JsonSerde<>(Greeting.class)
                ));


        var modifiedStream = greetingsStream
                //.mapValues((readOnlyKey, value) -> value.toUpperCase())
                .mapValues((readOnlyKey, value) -> new Greeting(value.getMessage().toUpperCase(), value.getTimeStamp()))
                ;


        modifiedStream
                //.print(Printed.<String, String>toSysOut().withLabel("greeting-streams"));
        .print(Printed.<String, Greeting>toSysOut().withLabel("greeting-streams"));

        modifiedStream
                .to(GREETINGS_OUTPUT,
                        Produced.with(Serdes.String(), new JsonSerde<Greeting>()));

    }
}
