package solution;

import javaslang.Function1;
import javaslang.Function2;
import javaslang.Function4;
import javaslang.control.Option;
import utils.Console;
import utils.Important;
import utils.Run;

public class LiveFunctions {

    @Run @Important
    public static void functionMemoization() {
        // TODO
    }

    public static void functionMemoizationSolution() {
        Function2<Integer, Integer, Integer> add = (a, b) -> {
            Console.log("Computing", a, "+", b);
            return a + b;
        };

        add.apply(1, 2);
        add.apply(1, 2);

        Function2<Integer, Integer, Integer> addMemo = add.memoized();

        addMemo.apply(1, 2);
        addMemo.apply(1, 2);
        addMemo.apply(1, 2);
        addMemo.apply(1, 2);
        addMemo.apply(1, 2);
    }

    @Run @Important
    public static void functionLifting() {
        // TODO
    }

    public static void functionLiftingSolution() {
        Function2<Integer, Integer, Integer> divide = (a, b) -> a / b;
        Function2<Integer, Integer, Option<Integer>> divideSafe = Function2.lift(divide);
        divideSafe.apply(2, 0);
    }

    public static void partialApplication()  {
        Function4<Integer, Integer, Integer, Integer, Integer> add = (a, b, c, d) -> a + b + c + d;
        Function1<Integer, Integer> add3 = add.apply(1, 1, 1);
        Console.log(add3.apply(3)); // 6
    }

    public static void curriedFunction()  {
        Function2<Integer, Integer, Integer> add = (a, b) -> a + b;
        Function1<Integer, Integer> add3 = add.curried().apply(3);
        Console.log(add3.apply(2)); // 5
    }
}
