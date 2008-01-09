#summary Readme first
=Testability Metric Explained=

==Basic Idea==
The testability score represents the difficulty of testing a particular
piece of code. The difficulty is a measure of:
  # how hard it will be to construct this object
  # how hard will it be to execute the method in order to be able to assert something in a test.

An example score for a method is:
  `methodName()V[1, 2 / 3, 4]`

The four numbers, in order, represent this method's:
  # _Testability Complexity_:
  # _Global State Complexity_:
  # _Total Testability Complexity_:
  # _Total Global State Complexity_: 

==Simplest Example==
Let's start with a simple example of analyzing a simple class.

*SOURCE:*
{{{
public class Primeness {
  public boolean isPrime(int number) {
    for (int i = 2; i < number / 2; i++) {
      if (number % i == 0) {
        return false;
      }
    }
    return true;
  }
}
}}}
*TRY IT:*`testability.sh -printDepth 10 com.google.test.metric.example.Primeness`

*OUTPUT:*
{{{
com.google.test.metric.example.Primeness
  <init>()V[0, 0 / 0, 0]
  isPrime(I)Z[2, 0 / 2, 0]
}}}

*EXPLANATION:*
{{{
com.google.test.metric.example.Primeness
                            | Name of the class
  <init>()V[0, 0 / 0, 0]    | Name of the method under test 
                            |   The <init> notation refers to the 
                            |   constructor.
                            | The [0, 0 / 0, 0] refers to the metric
                            | [tc, gc / ttc, tgc]
                            | (tc,gc) is per method complexity
                            | (ttc, tgc) total/recursive complexity
                            | tc: test complexity
                            | gc: global state complexity.
                            | ttc: Total test complexity (recursive)
                            | tgc: Total global complexity (recursive)
  isPrime(I)Z[2, 0 / 2, 0]  | This method has testing complexity of 2
                            | and has no global statics

}}}

==Test Complexity==
In the above example the test complexity is 2. This is because the
method `isPrime` has a loop and an `if` statement. Therefore there are 2
additional paths of execution for a total of 3.
 # Loop does not execute
 # Loop executes but if does not evaluate to true
 # Loop executes and if evaluates to true.

*Note:* Test cost is the method's cyclomatic complexity minus one. Subtract one
to not penalize the method for decomposing the method into 2 smaller methods. 
(If the lowest cost would be 1, splitting the method into two would result in 
the cost of 2.) The simplest method's score is 0 such that the method can be
split into smaller methods with no penalty.

==Example: Injectability Scoring==
This example shows the differences in scores based on how injectable a class is.
`SumOfPrimes1` directly instantiates a `new Primeness()`. The `new` operator
prevents you from being able to inject in a different subclass of `Primeness`
for testing. Thus the scores differ:
  * `sum(I)I[2, 0 / 4, 0]` <- total test complexity of 4 for `SumOfPrimes1`
  * `sum(I)I[2, 0 / 2, 0]` <- total test complexity of 2 for `SumOfPrimes2`

*SOURCE:*
{{{
public class SumOfPrimes1 {

  private Primeness primeness = new Primeness();
  
  public int sum(int max) {
    int sum = 0;
    for (int i = 0; i < max; i++) {
      if (primeness.isPrime(i)) {
        sum += i;
      }
    }
    return sum;
  }
  
}
}}}

*TRY IT:*
   `testability.sh -printDepth 10 com.google.test.metric.example.SumOfPrimes1`

*OUTPUT:*
{{{
com.google.test.metric.example.SumOfPrimes
  <init>()V[0, 0 / 0, 0]
  sum(I)I[2, 0 / 4, 0]
    line 25: isPrime(I)Z[2, 0 / 2, 0]
}}}

The testability and global state costs for the constructor (indicated by <init>)
is zero. 

The testability complexity of `sum` is 2, and the total testability complexity
is 4. The 2 comes from `sum` directly. The total of 4 is `sum`'s 2 plus 2 from 
`isPrime`.  The reason for this is that there is no way for a test to execute
the `sum` method and intercept the call to `isPrime` method. In other
words there is no way to write a true unit test which will test only the class
`SumOfPrimes1`. In this case it may not be a problem as the `isPrime` method is
not very expensive (in terms of complexity), however if the `isPrime` method
represented something expensive, like an external system, this would possess a
testing problem as there would be no way to test `sum` method without incurring
the cost of `isPrime`.

*SOURCE:*
{{{
public class SumOfPrimes2 {

  private final Primeness primeness;
  
  public SumOfPrimes2(Primeness primeness) {
    this.primeness = primeness;
  }

  public int sum(int max) {
    int sum = 0;
    for (int i = 0; i < max; i++) {
      if (primeness.isPrime(i)) {
        sum += i;
      }
    }
    return sum;
  }

}
}}}

*TRY IT:*
   `testability.sh -printDepth 10 com.google.test.metric.example.SumOfPrimes2`

*OUTPUT:*
{{{ 
com.google.test.metric.example.SumOfPrimes2
  <init>(Lcom/google/test/metric/example/Primeness;)V[0, 0 / 0, 0]
  sum(I)I[2, 0 / 2, 0]
}}}

In this case `Primeness` is injected the into the constructor of the 
`SumOfPrimes2`. As a result the cost of the `sum` method remains at 2, but the
cost of `isPrime` is now 0. (A mock of `Primeness` could be injected to test
`SumOfPrimes2`).

The cost of construction is added to the cost of testing the class. (You can 
only test a class which you can construct). In order to compute the cost we go 
through several phases:
  # Compute the cost of the constructor, giving zero cost to injectable variables 
  # Look for setter methods and use those to mark more fields as injectable.
  # Compute the cost of the method while respecting the injectability of fields, and method parameters.


==Injectability==

A variable (local variable, field or a parameter) is considered injectable if it
can be set from the outside (i.e. in the test). Any variable assigned from an
injectable variable is also considered injectable. In other words injectability 
is transitive. An Injectable variable can be replaced by a mock in test. Any 
method dispatched on an injectable variable has no cost. (It can be intercepted). 

_(Caveat: The method can not be static, private, or final, as those methods can not be 
overridden)._ 

==Example: Global State==

==Example: ... ==

==Future Enhancements / Requests==
Please talk about what you want on the mailing list:
http://groups.google.com/group/testability-metrics