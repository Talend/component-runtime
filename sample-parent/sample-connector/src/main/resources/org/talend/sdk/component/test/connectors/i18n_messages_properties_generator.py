#  Copyright (C) 2006-2025 Talend Inc. - www.talend.com
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

# This script allows to easily duplicate language files adding th language suffix to the text.
# Simply execute it

import os

# The specials_characters dict is used to add languages dependant specials characters to some entries for test purposes
# For now we only do it for the_family.TheOutput1._displayName
specials_characters = {"fr": u"é",
                       "uk": u"Ж",
                       "ja": u"愛",
                       "zh_CN": u"爱"}


class I18nMessagePropertiesGenerator:
    @staticmethod
    def add_suffix_to_properties_files(file, language_id: str):
        output_file = file.replace('Messages', 'Messages_' + language_id)
        special_character = specials_characters.get(language_id)

        with open(file, 'r', encoding='utf-8') as f:
            lines = f.readlines()

        with open(output_file, 'w', encoding='utf-8') as f:
            for line in lines:
                # Ignore blank lines and comments
                if line.strip() and not line.startswith('#'):

                    f.write(line.strip() + ' -' + language_id)

                    if line.__contains__('._documentation'):
                        # Documentation shall always end with a '.'
                        f.write(".")

                    if line.__contains__('the_family.TheOutput1._displayName') and special_character:
                        # Inject special character for test purpose

                        f.write(' +')
                        f.write(special_character)

                    f.write('\n')

                else:
                    f.write(line)

    def main(self):

        suffixes = ['en', 'fr', 'ja', 'uk', 'zh_CN']

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
