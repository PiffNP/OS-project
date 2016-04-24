#include "syscall.h"
#include "stdio.h"
int main(){
    char* argv[3];
    argv[0]="[i am first arg]";
    argv[1]="[i am second arg]";
    argv[2]="[i am third arg]";
    int id=exec("exec_test_child.coff",3,argv);
    int* x;
    int result=join(id,x);
    printf("id:%d\n",id);
    printf("result=%d return value=%d\n",id,*x);
    //halt();
}