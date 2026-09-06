import liquidjava.specification.Refinement;

class Warning {
    void run(@Refinement("_ > 0 && _ < 0") int value) {}
}
