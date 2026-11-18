# Math Lab: the demo story

A guided tour of magid-math, as a Magid story:

- four experiments: formulas, function tags and statistics, precision, print styles;
- a safe and a strict calculator, both taking your own formulas and numbers;
- an "About the Lab" scene.

It is styled as an instrument panel. In the web clients, a picture of each experiment's MagidML sits beside it
when the screen has room.

```
demo/
  mathlab/              the story: copy this folder into the server's content directory
    mathlab.xml
    bundle/css/         web.mathlab.css (browser, React), fx.mathlab.css (JavaFX)
    bundle/img/         the code pictures
  pictures/             how the pictures are made (CodeShot.java and the code of each)
```

## Installing it on a Magid server

You need a Magid server (nano or Spring Boot) and its `magid.properties`.

**1. Install the pack.** Stop the server first: a running server keeps its packs open.

With the `magid` command, straight from GitHub:

```bash
magid tagpack install https://github.com/tearim/magid-module-math/releases/download/v0.1.0/magid-math-0.1.0.jar
```

- The pack goes into the server's plugin directory, `magid.plugins` in its `magid.properties`. To name the
  directory yourself, add `--plugins <dir>`.
- `magid` downloads over https only and checks the pack as the server will before it installs it.
- If a magid-math is already installed, add `--replace`.

Without the `magid` command, download the jar from the
[release](https://github.com/tearim/magid-module-math/releases/tag/v0.1.0) and copy it into the plugin directory as
`magid-math.jar`.

**2. Copy the story.** Copy the folder `demo/mathlab` into the server's content directory (`magid.content.dir`), so
the story is `<content>/mathlab/mathlab.xml`. The release also has it as `math-lab.zip`: unzip it there.

**3. Check the server's settings** in `magid.properties`:

```properties
# the calculators' input fields
magid.allow-inputs=true
# headings, code, warnings in the text
magid.basic-html-tags=true
magid.basic-html-tags.allow-list=b,code,em,h2,hr,i,small,strong
magid.basic-html-tags.attributes-list=style,class,id,src
```

Without the HTML tags, the story still plays, with plainer text. Without inputs, only the calculators lose their
fields.

**4. Start the server** and choose **Math Lab**.

## Good to know

- **The strict calculator stops the story on purpose.** It shows what an error does: a bad formula, or a division by
  zero, ends the story with the tag's message. Restart the story to return to the lab. The safe calculator reports
  the same errors and goes on.
- **Degrees:** type `30deg` (or `30°`): `sin(30deg)` is exactly `0.5`.
- **Typed numbers** pass through the engine, which keeps about 7 digits of them. Results inside the pack are full
  doubles.
- **The pictures** show from a window width of 1100 px, beside the text, in the browser and React clients. The
  JavaFX client has no media queries and may show them inside the text.

## Remaking the pictures

Each picture is the code in `pictures/<name>.txt` (first line: the window title), drawn by `CodeShot.java`:

```bash
cd demo/pictures
javac CodeShot.java
java -Djava.awt.headless=true CodeShot formulas functions precision styles safe strict about
mv *.png ../mathlab/bundle/img/
```

It uses Consolas where Windows has it, and the JDK's monospaced font elsewhere.
