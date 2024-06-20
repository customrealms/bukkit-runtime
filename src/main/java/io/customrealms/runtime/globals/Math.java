package io.customrealms.runtime.globals;

import io.customrealms.runtime.Global;

import javax.script.Bindings;
import java.util.HashMap;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Math implements the JavaScript `Math` global.
 * @see <a href="https://developer.mozilla.org/en-US/docs/Web/JavaScript/Reference/Global_Objects/Math">MDN Docs</a>
 */
public class Math implements Global {
    public void init(Bindings bindings) {
        HashMap<String, Object> math = new HashMap<>();
        // Static properties
        math.put("E", java.lang.Math.E);
        math.put("LN10", java.lang.Math.log(java.lang.Math.E));
        math.put("LN2", java.lang.Math.log(2));
        math.put("LOG10E", java.lang.Math.log10(java.lang.Math.E));
        math.put("LOG2E", Math.log2(java.lang.Math.E));
        math.put("PI", java.lang.Math.PI);
        math.put("SQRT1_2", java.lang.Math.sqrt(0.5));
        math.put("SQRT2", java.lang.Math.sqrt(2));

        // Static methods
        math.put("abs", (Function<Double, Double>)java.lang.Math::abs);
        math.put("acos", (Function<Double, Double>)java.lang.Math::acos);
        math.put("acosh", (Function<Double, Double>)Math::acosh);
        math.put("asin", (Function<Double, Double>)java.lang.Math::asin);
        math.put("asinh", (Function<Double, Double>)Math::asinh);
        math.put("atan", (Function<Double, Double>)java.lang.Math::atan);
        math.put("atan2", (BiFunction<Double, Double, Double>)java.lang.Math::atan2);
        math.put("cbrt", (Function<Double, Double>)java.lang.Math::cbrt);
        math.put("ceil", (Function<Double, Double>)java.lang.Math::ceil);
        // math.put("clz32", (Function<Double, Double>)java.lang.Math::clz32);
        math.put("cos", (Function<Double, Double>)java.lang.Math::cos);
        math.put("cosh", (Function<Double, Double>)java.lang.Math::cosh);
        math.put("exp", (Function<Double, Double>)java.lang.Math::exp);
        math.put("expm1", (Function<Double, Double>)java.lang.Math::expm1);
        math.put("floor", (Function<Double, Double>)java.lang.Math::floor);
        // math.put("f16round", (Function<Double, Double>)java.lang.Math::f16round);
        // math.put("fround", (Function<Double, Double>)java.lang.Math::fround);
        math.put("hypot", (BiFunction<Double, Double, Double>)java.lang.Math::hypot);
        // math.put("imul", (Function<Double, Double>)java.lang.Math::imul);
        math.put("log", (Function<Double, Double>)java.lang.Math::log);
        math.put("log10", (Function<Double, Double>)java.lang.Math::log10);
        math.put("log1p", (Function<Double, Double>)java.lang.Math::log1p);
        math.put("log2", (Function<Double, Double>)Math::log2);
        math.put("max", (BiFunction<Double, Double, Double>)java.lang.Math::max);
        math.put("min", (BiFunction<Double, Double, Double>)java.lang.Math::min);
        math.put("pow", (BiFunction<Double, Double, Double>)java.lang.Math::pow);
        math.put("random", (Supplier<Double>)java.lang.Math::random);
        math.put("round", (Function<Double, Long>)java.lang.Math::round);
        math.put("sign", (Function<Double, Integer>)Math::sign);
        math.put("sin", (Function<Double, Double>)java.lang.Math::sin);
        math.put("sinh", (Function<Double, Double>)java.lang.Math::sinh);
        math.put("sqrt", (Function<Double, Double>)java.lang.Math::sqrt);
        math.put("tan", (Function<Double, Double>)java.lang.Math::tan);
        math.put("tanh", (Function<Double, Double>)java.lang.Math::tanh);
        // math.put("trunc", (Function<Double, Double>)java.lang.Math::trunc);

        bindings.put("Math", math);
    }

    /**
     * Releases all the values tying the runtime to the plugin
     */
    public void release() {}

    private static double acosh(double x) {
        if (x < 1.0) {
            throw new IllegalArgumentException("input to acosh must be >= 1");
        }
        return java.lang.Math.log(x + java.lang.Math.sqrt(x * x - 1));
    }

    private static double asinh(double x) {
        return java.lang.Math.log(x + java.lang.Math.sqrt(x * x + 1));
    }

    private static double log2(double x) {
        return java.lang.Math.log(x) / java.lang.Math.log(2);
    }

    private static int sign(double x) {
        return x == 0 ? 0 : x > 0 ? 1 : -1;
    }
}
