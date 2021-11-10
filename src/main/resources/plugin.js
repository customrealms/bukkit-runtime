class Plugin {

    onEnable() {
        console.log('Enabled!');

        let i = 0;
        setInterval(() => {
            i++;
            Java.resolve('org.bukkit.Bukkit').broadcastMessage('Hello #' + i);
        }, 5000);
    }

    onDisable() {
        console.log('Disabled!');
    }

}

_bootstrap(new Plugin());
