#  Copyright (C) 2006-2022 Talend Inc. - www.talend.com
#
#  Licensed under the Apache License, Version 2.0 (the "License");
#  you may not use this file except in compliance with the License.
#  You may obtain a copy of the License at
#
#      http://www.apache.org/licenses/LICENSE-2.0
#
#  Unless required by applicable law or agreed to in writing, software
#  distributed under the License is distributed on an "AS IS" BASIS,
#  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
#  See the License for the specific language governing permissions and
#  limitations under the License.
# Here you can change all your configuration display names to use more explicit labels
# You can also translate your configuration by adding one file by local Messages_fr.properties for french for example

# This script allows to easyly duplicate language files adding th language suffix to the text.
# Simply execute it

import os


class I18nMessagePropertiesGenerator:
    @staticmethod
    def add_suffix_to_properties_files(file, suffix):
        output_file = file.replace('Messages', 'Messages_' + suffix)

        with open(file, 'r') as f:
            lines = f.readlines()

        with open(output_file, 'w') as f:
            for line in lines:
                # Ignore blank lines and comments
                if line.strip() and not line.startswith('#'):
                    if line.__contains__('._documentation'):
                        f.write(line.strip() + ' -' + suffix + '.\n')
                    else:
                        f.write(line.strip() + ' -' + suffix + '\n')
                else:
                    f.write(line)

    def main(self):

        suffixes = ['en', 'fr', 'zh', 'ja', 'uk']

        # Get all "Messages.properties" files in the current directory and its subdirectories
        properties_files = []
        for root, dirs, files in os.walk('.'):
            for file in files:
                if file.startswith('Messages') and file.endswith('.properties') and not file.__contains__('_'):
                    properties_files.append(os.path.join(root, file))

        # Print the list of files, one file per line
        print("Processed files:")
        for file in properties_files:
            print('- ' + file)

        # Print the list of languages, one file per line
        print("Processed languages:")
        for language in suffixes:
            print('- ' + language)

        # For each file, add the given suffix to the end of each property
        for file in properties_files:
            for suffix in suffixes:
                self.add_suffix_to_properties_files(file, suffix)


if __name__ == '__main__':
    generator = I18nMessagePropertiesGenerator()
    generator.main()
