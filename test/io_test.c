#include "syscall.h"
#include "stdio.h"
int main(){
    int fd=creat("test.txt");
    char buffer[100]="[write this into the file]\n";
    printf("first fd=%d\n",fd);
    write(fd,buffer,100);
    close(fd);
    fd=open("test.txt");
    printf("second fd=%d\n",fd);
    int read_byte=read(fd,buffer,100);
    printf("read %d bytes.\n", read_byte);
    printf(buffer);
    close(fd);
    unlink("test.txt");
    fd=open("test.txt");
    printf("third fd=%d\n",fd);
}