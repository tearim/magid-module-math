# magid-math

A [Magid](https://github.com/tearim/magid-api) tagpack: arithmetic, rounding, statistics and trigonometry for MagidML
stories, in **double precision**, kept.

```xml
<config>
    <extend with="magid-math"/>
</config>
…
<description>
    The ladder reaches <math-calc digits="2">sqrt($wall^2 + $gap^2)</math-calc> metres.
    Your average roll: <math-mean values="$rolls"/>.
</description>
```

- Pack id `magid-math`, prefix `math` (tags read `<math-…>`; a story may choose another prefix with
  `<extend with="magid-math" as="m"/>`, or none with `as="none"`).
- Needs `magid-api` `[0.1,0.2)`.
- License: Apache 2.0.

## The precision promise

Every result is a double computed by IEEE 754 arithmetic (correctly rounded) or by `java.lang.StrictMath` (the same
bits on every machine and JVM). Concretely:

- **Nothing is lost between math tags.** A result stored in a symbol keeps every bit: an integer as an integer, any
  other value as the shortest text that reads back to the same double (`0.30000000000000004`). The next math tag
  reads it back exactly.
- **Outside the pack, no promise.** The engine's own numbers are floats; JEXL or another pack that computes with a
  stored value may round it to a float. Reading a stored value (printing it, `{$x}`) is harmless.
- **Sums don't drift.** Aggregates use Neumaier's compensated summation: `sum(1e100, 1, -1e100)` is `1`, and ten
  times `0.1` is `1`.
- **Degrees are exact where a story expects it.** `sin(30°)` is `0.5`, `cos(90°)` is `0`, `tan(45°)` is `1`,
  `asind(0.5)` is `30` (converting to radians first gives `0.49999999999999994`).
- **No NaN, no infinity.** A result that isn't a finite number is an error (see *Errors*).

## Tags

Every computing tag takes:

| Attribute | Meaning |
|---|---|
| `receiver` | A symbol to store the full result in: `$x`, `#x`, `player.gold`. With a receiver, nothing is printed… |
| `print="true"` | …unless this is set too. |
| `digits` | Decimals to print for this tag only (the stored value keeps every digit). |

Without a receiver, the result is printed: inline in a `<description>`, narrated in an `<action>`.

### `<math-calc>`: formulas

```xml
<math-calc>2 + 3 * 4</math-calc>                            14
<math-calc receiver="$hyp">sqrt($a^2 + $b^2)</math-calc>    stores, prints nothing
<math-calc>sin(30°) * $r + floor($x)</math-calc>
<math-calc>mean($scores) + {player.gold}</math-calc>
```

| Syntax | |
|---|---|
| `+ - * / % ^` | `%` is mod (the remainder has the divisor's sign); `^` is right-associative, `-2^2` is `-4` |
| `× · ÷ −` | accepted for `* * / -` |
| `30°` | degrees; `sin(30°)`, `cos(…°)`, `tan(…°)` compute exactly in degrees |
| `pi`, `e`, `tau` | constants |
| `name(a, b, …)` | any function below |
| `$x`, `#x`, `actor.key`, `actor::key` | the story's symbols, read as numbers (a list where an aggregate takes it) |
| `{…}`, `$(…)` | expanded by the engine before the formula is read |

Numbers are written with a dot (`1.5`, `.5`, `1.5e-3`), whatever the print style.

### One tag per function

Every function is also a tag, its parameters as attributes. Each attribute is itself a formula:

```xml
<math-sqrt x="2"/>                             1.4142135623730951
<math-round x="2.675" decimals="2"/>           2.68
<math-div a="{player.gold}" b="3" receiver="$share"/>
<math-pow a="$x + 1" b="2"/>
<math-mean values="1, 3, {x}, $scores"/>
<math-pi digits="4"/>                          3.1416
```

| Group | Functions (tag attributes) |
|---|---|
| Constants | `pi`, `e`, `tau` |
| Arithmetic | `add`, `sub`, `mul`, `div`, `mod` (floor), `rem` (truncated), `pow`, `hypot` (`a`, `b`); `abs`, `sign` (`x`) |
| Rounding | `floor`, `ceil`, `trunc`, `rint` (half to even) (`x`); `round` (`x`, `decimals`: half away from zero, on the decimal you wrote: `2.675` → `2.68`); `clamp` (`x`, `min`, `max`) |
| Powers, logarithms | `sqrt`, `cbrt`, `exp`, `ln`, `log10`, `log2` (`x`); `log` (`x`, `base`). Exact for powers of 2 and 10 |
| Trigonometry, radians | `sin`, `cos`, `tan`, `asin`, `acos`, `atan`, `sinh`, `cosh`, `tanh` (`x`); `atan2` (`y`, `x`) |
| Trigonometry, degrees | `sind`, `cosd`, `tand`, `asind`, `acosd`, `atand` (`x`); `atan2d` (`y`, `x`); `degrees`, `radians` convert |
| Aggregates | `sum`, `product`, `count`, `mean`, `median`, `min`, `max`, `varp`, `vars`, `stdevp`, `stdevs` (`values`) |

`varp`/`stdevp` are the population variance and deviation, `vars`/`stdevs` the sample ones.

### Lists

An aggregate's `values` is a comma-separated list of formulas: `values="1, $x, sqrt(2)"`. A symbol holding a list
(text like `2, 4, 9`) spreads out where a list goes: `<math-mean values="$rolls"/>`, `mean(1, $rolls)`.

### `<math-print-style/>`: how results print

```xml
<math-print-style digits="2"/>                    3.14; 2.50 prints 2.5
<math-print-style digits="2" trim="false"/>       2.50
<math-print-style decimal=","/>                   2,5
<math-print-style notation="scientific"/>         1.5E3
<math-print-style/>                               back to the defaults
```

| Attribute | Default | |
|---|---|---|
| `digits` | all the double has | decimals, rounded half away from zero |
| `trim` | `true` | drop trailing zeros |
| `notation` | `auto` | `auto` (plain unless below 10⁻⁶ or from 10¹⁵), `plain`, `scientific` |
| `decimal` | `.` | `.` or `,`; only printing changes |

By default an integer prints as `3`, never `3.0`, and other values print every digit they have. The style holds for
the rest of the session and is saved with it.

### `<math-errors reaction="…"/>`: errors

A division by zero, a formula that doesn't parse, `sqrt(-1)`, a result too large for a double:

- `reaction="throw"` (the default): the tag throws (`TagAttributeException` naming the attribute, or
  `TagException`), and the engine reports it with the tag's position.
- `reaction="silent"`: the tag prints nothing and leaves its receiver alone; the message goes into
  `$math_last_error`, which the next successful tag clears. A story can show it or test it.

A malformed `<math-print-style>` or `<math-errors>` always throws: that is a mistake in the story, not a calculation.

### Symbols the pack keeps

`$math_print_style`, `$math_errors`, `$math_last_error`. They are named with the pack's prefix and saved with the
session; don't write them yourself.

## Installing

```bash
magid tagpack install magid-math-0.1.0.jar         # into the server's plugin directory
```

Then `<extend with="magid-math"/>` in a story's `<config>`. To develop against a server, point its
`magid.plugins.dev` at this repository's `target/` (with `magid.dev=true`).

## Building

```bash
mvn verify
```

`magid-api` comes from GitHub Packages, which needs a token even for public packages: add a server `github` with
your GitHub user and a token (scope `read:packages`) to `~/.m2/settings.xml`, or `mvn install` a clone of
[magid-api](https://github.com/tearim/magid-api) first.

## Credits

- **Mark-Marion Gondelman**: Magid, the design of this pack (orchestration).
- **Claude Code** (Anthropic): agentic implementation.

Licensed under the [Apache License 2.0](LICENSE).
