import subprocess
import sys

def instrument(project:str,path:str):
    result=subprocess.run(['java','-cp','/root/project/greybox-APR/build/libs/APR-instrumenter.jar',
                        'kr.ac.unist.apr.InstrumentationMain','/root/project/greybox-APR/build/classes/java/main',
                        f'{path}/{project}/target/classes'])
    if result.returncode != 0:
        print(f'Instrumentation failed for {project}')
        exit(1)

    result2=subprocess.run(['cp','/root/project/greybox-APR/build/classes/java/main/kr/ac/unist/apr/GlobalStates.class',
                            f'{path}/{project}/target/classes/kr/ac/unist/apr/GlobalStates.class'])
    if result2.returncode != 0:
        print(f'Copy original code failed for {project}')
        exit(1)

if __name__ == '__main__':
    if len(sys.argv) != 3:
        print('Usage: python3 instrument.py <project_name> <target_path')
        exit(1)
    instrument(sys.argv[1],sys.argv[2])