#summary Readme first
=Testability Metric Explained=

==Basic Idea==
The testability score represents the difficulty of testing a particular
piece of code. The difficulty is a measure of:
  # how hard it will be to construct this object
  # how hard will it be to execute the method in order to be able to assert something in a test.

==How to run it==
{{{
$ java -jar testability-metrics-1.0.0RC2.jar

Argument "classes and packages" is required

 classes and packages : Classes or packages to analyze. Matches any class starting with these.
                        Ex. com.example.analyze.these com.google.and.these.packages com.google.AClass
 -costThreshold N     : Minimum Total Class cost required to print that class' metrics.
 -cp VAL              : colon delimited classpath to analyze (jars or directories)
                        Ex. lib/one.jar:lib/two.jar
 -printDepth N        : Maximum depth to recurse and print costs of classes/methods that the classes under analysis depe
                        nd on. Defaults to 0.
 -whitelist VAL       : colon delimited whitelisted packages that will not count against you. Matches packages/classes s
                        tarting with given values. (Always whitelists java.*)
}}}


==Output Format==
An example score for a method is:
  `package.Class.methodName()V[1, 2 / 3, 4]`

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
*TRY IT:
   `testability.sh -printDepth 10 com.google.test.metric.example.Primeness`

*OUTPUT:*
{{{
-----------------------------------------
Packages/Classes To Enter: 
 com.google.test.metric.example.Primeness*
-----------------------------------------


Testability cost for com.google.test.metric.example.Primeness [ 2 TCC, 0 TGC ]
  com.google.test.metric.example.Primeness.isPrime(I)Z [2, 0 / 2, 0]

-----------------------------------------
Summary Statistics:
 TCC for all classes entered: 2
 TGC for all classes entered: 0
 Average TCC for all classes entered: 2.00
 Average TGC for all classes entered: 0.00

Key:
 TCC: Total Compexity Cost
 TGC: Total Global Cost

Analyzed 1 classes (plus non-whitelisted external dependencies)
}}}

*EXPLANATION:*

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
-----------------------------------------
Packages/Classes To Enter: 
 com.google.test.metric.example.SumOfPrimes1*
-----------------------------------------


Testability cost for com.google.test.metric.example.SumOfPrimes1 [ 4 TCC, 0 TGC ]
  com.google.test.metric.example.SumOfPrimes1.sum(I)I [2, 0 / 4, 0]
    line 25: com.google.test.metric.example.Primeness.isPrime(I)Z [2, 0 / 2, 0]

-----------------------------------------
Summary Statistics:
 TCC for all classes entered: 4
 TGC for all classes entered: 0
 Average TCC for all classes entered: 4.00
 Average TGC for all classes entered: 0.00

Key:
 TCC: Total Compexity Cost
 TGC: Total Global Cost

Analyzed 1 classes (plus non-whitelisted external dependencies)
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
-----------------------------------------
Packages/Classes To Enter: 
 com.google.test.metric.example.SumOfPrimes2*
-----------------------------------------


Testability cost for com.google.test.metric.example.SumOfPrimes2 [ 2 TCC, 0 TGC ]
  com.google.test.metric.example.SumOfPrimes2.sum(I)I [2, 0 / 2, 0]

-----------------------------------------
Summary Statistics:
 TCC for all classes entered: 2
 TGC for all classes entered: 0
 Average TCC for all classes entered: 2.00
 Average TGC for all classes entered: 0.00

Key:
 TCC: Total Compexity Cost
 TGC: Total Global Cost

Analyzed 1 classes (plus non-whitelisted external dependencies)
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

Global state makes it hard to tests ones code as it allows cross talk between
test. This makes it so that tests can pass by themselves but fail when run in 
a suite. It is also possible to make tests which will run in only a specific
order.

*SOURCE:*
{{{
package com.google.test.metric.example;

public class GlobalExample {

  public static class Gadget {
    private final String id;
    private int count;

    public Gadget(String id, int count) {
      this.id = id;
      this.count = count;
    }

    public String getId() {
      return id;
    }

    public int getCount() {
      return count;
    }

    public int increment() {
      return ++count;
    }
  }

  public static class Globals {
    public static final Gadget instance = new Gadget("Global", 1);
  }

  public Gadget getInstance() {
    return Globals.instance;
  }

  public String getGlobalId() {
    return Globals.instance.getId();
  }

  public int getGlobalCount() {
    return Globals.instance.getCount();
  }

  public int globalIncrement() {
    return Globals.instance.increment();
  }
}
}}}

*TRY IT:*
   `testability.sh -printDepth 10 com.google.test.metric.example.GlobalExample com.google.test.metric.example.GlobalExample`

*OUTPUT:*

{{{
-----------------------------------------
Packages/Classes To Enter: 
 com.google.test.metric.example.GlobalExample*
-----------------------------------------


Testability cost for com.google.test.metric.example.GlobalExample [ 0 TCC, 2 TGC ]
  com.google.test.metric.example.GlobalExample.getGlobalCount()I [0, 0 / 0, 1]
    line 55: com.google.test.metric.example.GlobalExample$Gadget.getCount()I [0, 1 / 0, 1]
  com.google.test.metric.example.GlobalExample.globalIncrement()I [0, 0 / 0, 1]
    line 59: com.google.test.metric.example.GlobalExample$Gadget.increment()I [0, 1 / 0, 1]

Testability cost for com.google.test.metric.example.GlobalExample$Globals [ 0 TCC, 3 TGC ]
  com.google.test.metric.example.GlobalExample$Globals.<init>()V [0, 0 / 0, 1]
    line 43: com.google.test.metric.example.GlobalExample$Globals.<clinit>()V [0, 1 / 0, 1]
  com.google.test.metric.example.GlobalExample$Globals.<clinit>()V [0, 2 / 0, 2]

-----------------------------------------
Summary Statistics:
 TCC for all classes entered: 0
 TGC for all classes entered: 5
 Average TCC for all classes entered: 0.00
 Average TGC for all classes entered: 1.67

Key:
 TCC: Total Compexity Cost
 TGC: Total Global Cost

Analyzed 3 classes (plus non-whitelisted external dependencies)
}}}

===`getGlobalId()`===

Method `getGlobalId()` has no global cost. This may be surprising given that
it accesses static variables. However, upon closer examinations, these
variables are not mutable (they are declared `final`). For this reason there
is no global mutable state associated with this method.

===`getInstance()`===

Similarly `getInstance()` has no global cost either as it only accesses
variables which are final.

===`getGlobalCount()`===

Method `getGlobalCount()` accesses the `Globals.instance` which is a global
constant. Accessing global constants is not a problem and hence does not 
incur a cost. It then calls the `Gadget.getCount()` which accesses the `count`
field. Because the `Gadget`'s `this` is a global constant, all of its
fields are global as well. The global property is transitive. Because  `count`
is mutable (no `final` keyword) reading `count` incurs a cost.

===`globalIncrement()`===

Method `getGlobalIncrement()` follows the same logic as `getGlobalCount()`.

==Future Enhancements / Requests==
Please talk about what you want on the mailing list:
http://groups.google.com/group/testability-metrics