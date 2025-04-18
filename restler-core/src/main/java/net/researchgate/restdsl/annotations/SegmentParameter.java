package net.researchgate.restdsl.annotations;

import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Schema;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.PARAMETER, ElementType.METHOD, ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
@Parameter(
        name = "segment",
        in = ParameterIn.PATH,
        schema = @Schema(type = "string", example = "-"),
        description = "A rest-dsl query, See https://github.com/researchgate/restler#get",
        required = true
)
public @interface SegmentParameter {
    String name() default "segment";
    String description() default "A rest-dsl query, See https://github.com/researchgate/restler#get";
    ParameterIn in() default ParameterIn.PATH;
    boolean required() default true;
}
