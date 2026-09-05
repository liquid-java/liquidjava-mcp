import liquidjava.specification.*;

@RefinementAlias("Nonnegative(int x) { x >= 0 }")
@Ghost("int count")
@RefinementPredicate("int total(Definitions value)")
@StateSet({"ready", "done"})
interface Definitions {}
