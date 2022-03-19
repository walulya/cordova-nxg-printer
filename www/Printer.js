var exec = require('cordova/exec');

var PLUGIN_NAME = 'PrinterHelper';

var NXGOPrinter = {
    add: function (args, cb) {
        //exec(cb, null, PLUGIN_NAME, 'echo', [phrase]);
        exec(cb, null, PLUGIN_NAME, "performAdd", args);
    },
     printtest: function(fnSuccess, fnError){
        exec(fnSuccess, fnError, PLUGIN_NAME, "printtest", []);
     },
    printtext: function(fnSuccess, fnError, text){
        exec(fnSuccess, fnError, PLUGIN_NAME, "text", [text]);
     },
    init: function(fnSuccess, fnError){
        exec(fnSuccess, fnError, PLUGIN_NAME, "init", []);
     },
}

exports.coolMethod = function (arg0, success, error) {
    exec(success, error, PLUGIN_NAME, 'coolMethod', [arg0]);
};


module.exports = NXGOPrinter;