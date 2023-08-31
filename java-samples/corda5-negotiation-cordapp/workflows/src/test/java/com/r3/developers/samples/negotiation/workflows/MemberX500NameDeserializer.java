package com.r3.developers.samples.negotiation.workflows;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import net.corda.v5.base.types.MemberX500Name;

import java.io.IOException;

public class MemberX500NameDeserializer extends JsonDeserializer<MemberX500Name> {
    @Override
    public MemberX500Name deserialize(JsonParser parser, DeserializationContext ctxt) throws IOException {
        return MemberX500Name.parse(parser.getText());
    }
}
