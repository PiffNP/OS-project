#include "stdio.h"
#include "syscall.h"
int main(int argc, char** argv)
{
    printf("run child\n");
    int i;
    printf("%d arguments\n", argc);
    for (i=0; i<argc; i++)
        printf("arg %d: %s\n", i, argv[i]);
    exit(7);
    return 0;
}
