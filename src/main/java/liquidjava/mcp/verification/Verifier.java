package liquidjava.mcp.verification;

@FunctionalInterface
public interface Verifier {
    VerifyResult verify(VerifyRequest request);
}
