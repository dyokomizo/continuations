package br.com.sec.buzz.k;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.Charset;

import org.junit.Test;


public class ContinuationTest {
    private static final Charset UTF_8 = Charset.forName("UTF-8");


    @Test public void whatever() throws Exception {
        final F<String, Integer> atoi = new F<String, Integer>() {
            @Override public Integer f(String s) {
                return Integer.valueOf(s);
            }
        };
        final F<Integer, String> itoa = new F<Integer, String>() {
            @Override public String f(Integer i) {
                return Integer.toString(i);
            }
        };
        final F<Integer, F<Integer, Integer>> plus = new F<Integer, F<Integer, Integer>>() {
            @Override public F<Integer, Integer> f(final Integer i1) {
                return new F<Integer, Integer>() {
                    @Override public Integer f(Integer i2) {
                        return i1 + i2;
                    }
                };
            }
        };
        final K<Integer, String> k1 = readLineFrom(System.in);
        final K<Integer, Integer> k2 = liftM2(plus, map(k1, atoi), map(k1, atoi));
        final K<Integer, Void> k3 = printLine(System.out, map(k2, itoa));
        int theAnswer = eval(k3, new F<Void, Integer>() {
            @Override public Integer f(Void f) {
                System.out.println("Goodbye");
                return 42;
            }
        });
        System.out.println(theAnswer);
    }


    private static <T> K<T, Void> printLine(final OutputStream os, K<T, String> k2) {
        return bind(k2, new F<String, K<T, Void>>() {
            @Override public K<T, Void> f(String s) {
                return printLine(s, os);
            }
        });
    }


    private static <R, A, B> K<R, B> bind(final K<R, A> k1, final F<A, K<R, B>> fk2) {
        return new K<R, B>() {
            @Override public F<F<B, R>, R> run() {
                return new F<F<B, R>, R>() {
                    @Override public R f(final F<B, R> f) {
                        return k1.run().f(new F<A, R>() {
                            @Override public R f(A a) {
                                return fk2.f(a).run().f(f);
                            }
                        });
                    }
                };
            }
        };
    }


    private static <T> K<T, Void> printLine(String s, OutputStream os) {
        return printLine(s, new OutputStreamWriter(os, UTF_8));
    }


    private static <T> K<T, Void> printLine(final String s, Writer w) {
        return new K<T, Void>() {
            @Override public F<F<Void, T>, T> run() {
                return new F<F<Void, T>, T>() {
                    @Override public T f(F<Void, T> k) {
                        System.out.println(s);
                        return k.f(null);
                    }
                };
            }
        };
    }


    private <R, A> R eval(K<R, A> k, final F<A, R> f) {
        return k.run().f(f);
    }


    private <T> K<T, String> readLineFrom(InputStream is) {
        return readLineFrom(new BufferedReader(new InputStreamReader(is, UTF_8)));
    }


    private <T> K<T, String> readLineFrom(final BufferedReader br) {
        return new K<T, String>() {
            @Override public F<F<String, T>, T> run() {
                return new F<F<String, T>, T>() {
                    @Override public T f(F<String, T> k) {
                        try {
                            final String l = br.readLine();
                            return k.f(l);
                        } catch (IOException exc) {
                            throw new RuntimeException(exc);
                        }
                    }
                };
            }
        };
    }


    /**
     * map m f = m >>= \x -> return (f x)
     */
    static <R, A, B> K<R, B> map(K<R, A> m, final F<A, B> f) {
        return bind(m, new F<A, K<R, B>>() {
            @Override public K<R, B> f(A x) {
                return unit(f.f(x));
            }
        });
    }


    /**
     * liftM2 f ma mb= ma >>= \a -> mb >>= \b -> return (f a b)
     */
    private static <R, A, B, C> K<R, C> liftM2(final F<A, F<B, C>> f, K<R, A> ma, final K<R, B> mb) {
        return bind(ma, new F<A, K<R, C>>() {
            @Override public K<R, C> f(final A a) {
                return bind(mb, new F<B, K<R, C>>() {
                    @Override public K<R, C> f(B b) {
                        return unit(f.f(a).f(b));
                    }
                });
            }
        });
    }


    static <R, A> K<R, A> unit(final A n) {
        return new K<R, A>() {
            @Override public F<F<A, R>, R> run() {
                return new F<F<A, R>, R>() {
                    @Override public R f(F<A, R> k) {
                        return k.f(n);
                    }
                };
            }
        };
    }

    interface K<R, A> {
        F<F<A, R>, R> run();
    }

    interface F<F, T> {
        T f(F f);
    }
}
