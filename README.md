# Flight
[![Release](https://jitpack.io/v/devoxin/flight.svg)](https://jitpack.io/#devoxin/flight)
[![](https://jitci.com/gh/devoxin/flight/svg)](https://jitci.com/gh/devoxin/flight)

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

### Command Client
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

### Commands
Now for the part you've been waiting for -- commands!
Commands need to be contained inside a `Cog`. The cog serves not only as the container, but the category too -- so you can group commands based on what they do, for example; moderation. However we'll just be sticking with the basic `ping` command for now.
```kotlin
package my.bot.commands // Your package name most likely won't be this, but this is included as it's necessary for registering commands with the command client.

class YourCog : Cog { // "YourCog" will be used as the category name for commands within this cog.

  @Command(description = "Ping, pong!")
  fun ping(ctx: Context) {
    ctx.send("Pong!")
  }

}
```

#### Subcommands
Flight supports subcommands out-of-the-box, which can simplify command structure and parsing. Subcommands are functionally similar to commands, however there are a few restrictions. A subcommand's parent Cog **must** contain exactly **1** top-level command. Subcommands also inherit the permission requirements of their parent command. Subcommands **cannot** be attached to other subcommands.
Top-level commands support an infinite number of subcommands.
```kotlin
package my.bot.commands

class SubcommandDemo : Cog {

  // The permission requirement seen below is enforced for the top-level command, and any subcommands it may have.
  @Command(description = "Configure bot settings", userPermissions = [Permission.MANAGE_SERVER])
  fun settings(ctx: Context) {
    // This is the top-level command. It will only be invoked when a user does not specify a (valid) subcommand.
    ctx.send("Hey! You must specify a subcommand.\nAvailable subcommands:\n`prefix` `locale`")
  }
  
  @Subcommand(description = "Set the server prefix")
  fun prefix(ctx: Context, newPrefix: String) {
    Database.updatePrefix(ctx.guild!!.idLong, newPrefix)
    ctx.send("The prefix was changed to `$newPrefix`.")
  }
  
  @Subcommand(description = "Set the language of the bot for the server.")
  fun locale(ctx: Context, newLocale: String) {
    val selectedLocale = validateLocale(newLocale)
        ?: return ctx.send("That doesn't appear to be a valid locale.")
    Database.updateLocale(ctx.guild!!.idLong, selectedLocale)
    ctx.send("The locale was changed to ${selectedLocale.flag} `${selectedLocale.asIso}`.")
  }

}
```
To execute our new subcommand, we would need to type `!settings prefix`. To pass a prefix, we just need to provide a second argument: `!settings prefix ?`.
Change the `!` as necessary to match your bot's prefix.

#### Registering
You now have your first cog and command, all that's left to do is register it! This is as easy as the following:
```kotlin
commandClient.registerCommands("my.bot.commands")
```

You will need to change `my.bot.commands` to reflect the actual package name of where your commands are stored. If done correctly, all cogs and commands contained within the `my.bot.commands` package will be scanned and registered ready for use.
