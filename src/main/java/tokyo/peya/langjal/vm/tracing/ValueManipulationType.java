package tokyo.peya.langjal.vm.tracing;

public enum ValueManipulationType
{
    GENERATION,
    MANIPULATION,
    COMBINATION,
    DESTRUCTION,

    TO_LOCAL,
    FROM_LOCAL,

    PASSING_AS_ARGUMENT,
    RETURNING_FROM,

    FIELD_GET,
    FIELD_SET,
}
