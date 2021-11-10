console.log('Enabled!');

let i = 0;
setInterval(() => {
    i++;
    Java.resolve('org.bukkit.Bukkit').broadcastMessage('Hello #' + i);
}, 5000);

ServerCommands.register((uuid, message) => {

    console.log('UUID: ', uuid);
    console.log('MESSAGE: ', message);
    return true;

});
