const spawn = require('child_process').spawn;
const command = process.argv[2];
const args = process.argv.slice(3);
const child = spawn(command, args);
console.log(`> running command ${command} with args ${args}`);
 
const heartbeat = setInterval(() => {
  console.log('❤️');
}, 1000 * 60);
 
const timeout = setTimeout(() => {
  console.log('> heartbeat: child process timed out after 60 min');
  clearInterval(heartbeat);
  process.exit(1);
}, 1000 * 60 * 60);
 
child.stdout.on('data', (data) => {
  console.log(data.toString().replace(/\r?\n$/, ''));
});
 
child.stderr.on('data', (data) => {
  console.log(data.toString().replace(/\r?\n$/, ''));
});
 
child.on('close', (code) => {
  console.log(`heartbeat: child process exited with code ${code}`);
  clearInterval(heartbeat);
  clearTimeout(timeout);
  process.exit(code);
});

