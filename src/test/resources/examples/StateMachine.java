package examples;

import liquidjava.specification.StateRefinement;
import liquidjava.specification.StateSet;

@StateSet({"open", "closed"})
public class StateMachine {
    @StateRefinement(to = "open(this)")
    public StateMachine() {}

    @StateRefinement(from = "open(this)")
    public void read() {}

    @StateRefinement(from = "open(this)", to = "closed(this)")
    public void close() {}
}
