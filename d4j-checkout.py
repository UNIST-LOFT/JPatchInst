import subprocess
import sys

def checkout(project:str,path:str):
    subject, bug_id = project.split('_')
    subprocess.run(['wget',f'https://github.com/ali-ghanbari/d4j-{subject.lower()}/raw/master/{subject}-{bug_id}.tar.gz'])
    subprocess.run(['tar','-xf',f'{subject}-{bug_id}.tar.gz'])
    subprocess.run(['mv',f'{bug_id}',f'{path}/{project}'])
    subprocess.run('rm','-rf',f'{subject}-{bug_id}.tar.gz')

if __name__ == '__main__':
    if len(sys.argv) != 3:
        print('Usage: python3 d4j-checkout.py <project_name> <target_path')
        exit(1)
    checkout(sys.argv[1],sys.argv[2])