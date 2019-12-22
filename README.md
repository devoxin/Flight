# Flight
Flight is a lightweight yet powerful command handler and arg parser for JDA, in Kotlin. It offers numerous utilities and functions for assisting, and speeding up Discord Bot development, allowing you to spend less time on the boring bits, and more on the important bits.

## Quick Start
Using Flight is easy. The first thing you need to do before you can start diving in to bot making is to import it into your project.
```gradle
repositories {
  maven { url 'https://jitpack.io' }
}

dependencies {
  compile 'com.github.Devoxin:Flight:VERSION'
}
```
Make sure to replace `VERSION` with the latest Flight version. You can find a list of versions in the releases tab.

Now that importing Flight is out of the way, we can begin constructing our command client.
```kotlin
val commandClient = CommandClientBuilder()
  .setPrefixes("!", "?") // You can use multiple prefixes with Flight.
  .registerDefaultParsers() // This registers all default parsers that ship with Flight. This allows us to make use of Flight's arg-parsing capabilities.
  .build() // There is a lot more we can configure, but the default values are fine for our needs.

// Each builder option returns the Builder instance. This makes chaining calls easy.
```

Now that we have our command client, we need to create our JDA client to connect our bot to Discord so we can start receiving and processing events.
```kotlin
val jda = JDABuilder(yourBotToken)
  .addEventListeners(commandClient) // It's necessary to register our command client as an event listener so that it can process commands. This is also necessary for event waiting.
  .build()
```

Now for the part you've been waiting for -- commands!
Commands need to be contained inside a `Cog`. The cog serves not only as the container, but the category too -- so you can group commands up based on what they do, for example; moderation. However we'll just be sticking with the basic `ping` command for now.
```kotlin
package my.bot.commands // Your package name most likely won't be this, but this is included as it's necessary for registering commands with the command client.

class YourCog : Cog { // The name provided here is used as the category name for commands.

  @Command(description = "Ping, pong!")
  fun ping(ctx: Context) {
    ctx.send("Pong!")
  }

}
```

You now have your first cog and command, all that's left to do is register it! This is as easy as the following:
```kotlin
commandClient.registerCommands("my.bot.commands")
```

You will need to change `my.bot.commands` to reflect the actual package name of where your commands are stored, but if done correctly, all cogs and commands contained within the `my.bot.commands` package will be scanned and registered ready for use.
