import time
import serial
import numpy as np
import fnmatch



class espserial:
    
    '''
    This class will control the Lens and Laser  for the cellstorm through a serial port

    Available commands:
    
    All commands accepted by the ESP32:
    const char *CMD_DRVX =    "DRVX";
    const char *CMD_DRVY =   "DRVX";
    const char *CMD_DRVZ =   "DRVZ";
    const char *CMD_LENS1X =  "LENS1X";
    const char *CMD_LENS1Z =  "LENS1Z";
    const char *CMD_LENS2X =  "LENS2X";
    const char *CMD_LENS2Z =  "LENS2Z";
    const char *CMD_LAS1 =    "LAS1";
    const char *CMD_LAS2 =    "LAS2";
    const char *CMD_LED1 =    "LED1";
    const char *CMD_LX_SOFI =  "LX_SOFI";
    const char *CMD_LZ_SOFI =  "LZ_SOFI";
    const char *CMD_LX_SOFI_A =  "LX_SOFI_A";
    const char *CMD_LZ_SOFI_A =  "LZ_SOFI_A";
    '''

    board = 'ESP32'
    firmware = 'DEFAULT'
    pwmresolution = 2^10

    delim_strt = "*"
    delim_stop = "#"
    delim_cmds = ";"
    delim_inst = "+"
    
    is_debug = False

    def __init__(self, serialconnection = None, 
                port="/dev/USB0", 
                baud=115200,
                timeout=1):
        # either open a new connection or you have one already
        if serialconnection is None:
            self.serialconnection = serial.Serial(
            port=com,
            baudrate=baud,
            timeout=timeout,
            stopbits=serial.STOPBITS_ONE,
            bytesize=serial.EIGHTBITS,
            parity=serial.PARITY_NONE,
        )
        # Alternatively use an already established serial connection
        # e.g. if laser, lens and led are connected to one µControler
        self.serialconnection = serialconnection


    def close(self):
        """ CLose Serial Connection"""
        self.serialconnection.close()

    def initserial(self):
        """ Initiliazing the serial connection and set home coordinates """

    def auto_detect_serial_unix(self, preferred_list=['*']):
        '''try to auto-detect serial ports on posix based OS'''
        import glob
        glist = glob.glob('/dev/ttyUSB*') + glob.glob('/dev/ttyACM*')
        ret = []

        # try preferred ones first
        for d in glist:
            for preferred in preferred_list:
                if fnmatch.fnmatch(d, preferred):
                    #ret.append(SerialPort(d))
                    ret.append(d)
        if len(ret) > 0:
            return ret
        # now the rest
        for d in glist:
            #ret.append(SerialPort(d))
            ret.append(d)
        return ret


    ''' 
    Handle communication between python and ESP
    '''
    def extractCommand(self, args):
        """Decode received Command"""
        cmd = ""
        delim = self.delim_inst
        for i, arg in enumerate(args):
            if type(arg) == list:
                sep = [str(x) for x in arg]
                cmd += delim.join(sep)
            else:
                cmd += str(arg)
            cmd += delim

        return cmd[:-1]

    def send(self, *args):
        cmd = self.extractCommand(args)
        if(self.is_debug): print("Sending:   {0}".format(cmd))
        self.sendEvent(cmd)

    def _write(self, command):
        """Send a command."""
        response = self.serialconnection.write(command)
        return response

    def sendEvent(self, value):
        """Package and Send command"""
        try:
            self.outBuffer = self.delim_strt + str(value) + self.delim_stop
            print("Printing Buffer: "+self.outBuffer)
            self.outBuffer = [ord(x) for x in self.outBuffer]
            self._write(self.outBuffer)
            time.sleep(.001)
            self.outBuffer = "" # reset
            self._flush_handshake()
        except Exception as e:
            print(e)
        return



    def _readline(self):
        """Read a line from connection without leading and trailing whitespace.
        We override from SerialDeviceMixin
        """
        response = self.serialconnection.readline().strip()
        return response

    def _flush_handshake(self):
        self.serialconnection.readline()
        self.serialconnection.flushInput()


