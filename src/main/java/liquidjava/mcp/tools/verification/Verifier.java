package liquidjava.mcp.tools.verification;

@FunctionalInterface
public interface Verifier {
    VerifyResult verify(VerifyRequest request);
}
