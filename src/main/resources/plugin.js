class Plugin {

    enable() {
        console.log('Enabled!');

        let i = 0;
        setInterval(() => {
            i++;
            Java.resolve('org.bukkit.Bukkit').broadcastMessage('Hello #' + i);
        }, 5000);
    }

    disable() {
        console.log('Disabled!');
    }

}

_cr_bootstrap(new Plugin());
