How it Works

# Introduction #

For those who are interested how the numbers are computed you can find the inner workings here.

# What is Testable? #

## Injectability is Good ##
We consider a class testable if it would be easy for us to exercise all paths of execution of the class without exercising  the rest of the system. (The classic definition of 'unit test' only test one class at a time) In order to test only the class, we need to be able to intercept any calls going out of the class, (or at least be sure that those few calls which we can not intercept are inexpensive).

> Holding all other things equal, we believe that for two classes the one which allows interception of outbound calls will be easier to test, **because it can be isolated in a test**.

We intercept calls by:
  * Overriding a method in a subclass.
  * Working with a mock/fake implementation instead of real one.

In order to be able to override method, or pass in a mock, we must control object construction, so that we can construct a mock or a subclass of the class under test. Lets look at an example:
```
public class SumOfPrimes1 {
  private final Primeness primeness = new Primeness();
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
```

In the code above, there is no way to test `SumOfPrimes1` class without exercising `Primeness` class as well. This is because  we can not intercept the call to `primeness.isPrime()`. This is because in order to override the method we would need to pass in a subclass of `Primeness`, but the test does not control the construction of the `Primeness` and hence can not intercept it.

> In this case the cost of `primeness.isPrime()` is low and is not an issue. But imagine  if the the call talked to an external system and charged a credit card. In the real world, interception becomes top priority.

In the similar code below the call to `primeness.isPrime()` can be intercepted in the test. Primeness is set via the constructor, so the test can easily pass in a subclass of `Primeness` with its `isPrime()` method stubbed out. We therefore believe that this class is easier to test.
```
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
```

We say that the field `primeness` is **injectable**. This implies that any method dispatch (except `final`/`private`/`static`) on the `primeness` field  can be intercepted.

## Injectability is Transitive ##

The tool heuristically looks at a class and identifies all variables/fields/parameters which are injectable (i.e. can be controlled from the outside).

The heuristics to calculate injectability are:
  * Assume any public non final fields are injectable and mark them so.
  * Find the constructor with the most (non-primitive) arguments and analyze the assignments marking fields injectable as necessary.
  * Find all setters and analyze their assignments marking fields injectable as necessary.
  * Analyze the method of interest.
    * Compute the cyclomatic complexity**of the method this is the method cost.
    * If the method is instance method add the cost (cyclomatic complexity) of the constructor (There is no way to instantiate an object and not call its constructor)
    * Recursively add the cost of any method called from this method which are dispatched on a non-injectable instance. (Injectable instances can be intercepted and hence their cost can be avoided in test).**

  * OTE: The cyclomatic complexity used by the tools is decremented by 1. A standard way of computing cyclomatic complexity is to start at 1, but a method with a cyclomatic complexity of 1 can be split to `N` smaller methods. Splitting into methods would increase your complexity from 1 to N. This penalizes code which is broken into lots of small methods (but lots of small methods is a good thing!) For this reasons we changed the offset of the method and say that a simple method is 0 and hence splitting 0 to `N` is still zero.

# Global State is Undesirable #

Many software developers are of the opinion that a global state is undesirable. A few of the reasons are:
  * hidden dependencies
  * poor isolation of tests (Order of tests may matter)

> NOTE: It is not that all globals are bad, only those which are mutable. The immutable ones are constants for all practical purposes.


## Global variables are transitive ##

```
  public static class Gadget {
    public static final Gadget instance = new Gadget("Global", 1);
    public final String id;
    public int count;

    private Gadget(String id, int count) {
      this.id = id;
      this.count = count;
    }
  }
```

In the example above all global (`static`) fields are declared as `final` hence one would think that there is no mutable global state. But this is not true. The field `count`is mutable and is globally reachable `Gadget.instance.count`. therefore if there is code which `Gadget.instance.count` it will incur global cost. The fields `Gadget.instance` is `static` and hence is marked global. However, because it is `final` it is immutable. However, traversing `Gadget.instance` makes the `Gadget` instance global. Accessing any fields on `Gadget` will be considered global access and accessing any non `final` fields (either read or write) will incur a global cost.

# Summary #

We believe that high injectability and low global state leads to testable code. High injectability is good because it gives the test plenty of choices where to intercept the code under tests and make the test as small as possible. Similarly low global state will aid in isolating the tests from each other. Both of which are highly desirable qualities.