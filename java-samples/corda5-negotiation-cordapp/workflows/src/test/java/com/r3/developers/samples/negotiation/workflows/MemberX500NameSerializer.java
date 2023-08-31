package com.r3.developers.samples.negotiation.workflows;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import net.corda.v5.base.types.MemberX500Name;

import java.io.IOException;

public class MemberX500NameSerializer extends JsonSerializer<MemberX500Name> {
    @Override
    public void serialize(MemberX500Name value, JsonGenerator generator, SerializerProvider serializers) throws IOException {
        generator.writeString(value.toString());
    }
}
