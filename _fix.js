const fs = require("fs");
const f = "apps/api/src/modules/messaging/message.routes.ts";
let c = fs.readFileSync(f, "utf8");
c = c.replace(/
    \}\);
/g, "
");
fs.writeFileSync(f, c);
console.log("fixed");
