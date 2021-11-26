setInterval(() => {
    Java.resolve('org.bukkit.Bukkit').getServer().getOnlinePlayers().map(p => p.sendMessage('Hey'));
}, 2000);

// Simple example plugin that bans the player from breaking more than 1 of any block
// type within the same 10 seconds.

const banned_materials = [];

BukkitEvents.register('org.bukkit.event.block.BlockBreakEvent', e => {
    const material = e.getBlock().getType().name();
    if (banned_materials.indexOf(material) === -1) {
        // allow this break
        banned_materials.push(material);
        setTimeout(() => {
            const index = banned_materials.indexOf(material);
            if (index > -1) banned_materials.splice(index, 1);
        }, 10000);
    } else {
        e.setCancelled(true);
        e.getPlayer().sendMessage('Temporarily banned from breaking: ' + material);
    }
});

BukkitCommands.register((player, message) => {
    if (message.startsWith('/gmc')) {
        player.setGameMode(Java.resolve('org.bukkit.GameMode').valueOf('CREATIVE'));
        return true;
    } else if (message.startsWith('/gms')) {
        player.setGameMode(Java.resolve('org.bukkit.GameMode').valueOf('SURVIVAL'));
        return true;
    }
});

const http = require('http');
const server = http.createServer((req, res) => {
    res.writeHead(200, {'Content-Type': 'application/json'});
    res.end(JSON.stringify({
        version: process.version,
        players: Java.resolve('org.bukkit.Bukkit').getServer().getOnlinePlayers().map(p => p.getName()),
        process_keys: Object.keys(process).reduce((sum, key) => {
            sum[key] = typeof process[key];
            return sum;
        }, {}),
    }));
});
server.listen(8080);
console.log('Server started: http://localhost:8080');

