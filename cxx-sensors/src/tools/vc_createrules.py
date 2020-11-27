# pip install beautifulsoup4
# pip install selenium-requests

from bs4 import BeautifulSoup
from bs4.dammit import EntitySubstitution
from selenium import webdriver
import re
import sys

from utils_createrules import CDATA
from utils_createrules import write_rules_xml
from utils_createrules import get_cdata_capable_xml_etree


# url of overview pages and default values
#  add more than one tag as a comma separated string: 'tag': 'a,b,c'
URLS = {
    'https://docs.microsoft.com/en-us/cpp/error-messages/compiler-warnings/compiler-warning-level-4-c4001' : {
        'severity': 'INFO',
        'type': 'CODE_SMELL',
        },
     'https://docs.microsoft.com/en-us/cpp/error-messages/compiler-warnings/compiler-warning-levels-2-and-4-c4200' : {
        'severity': 'INFO',
        'type': 'CODE_SMELL',
        },
     'https://docs.microsoft.com/en-us/cpp/error-messages/compiler-warnings/compiler-warning-level-4-c4400' : {
        'severity': 'INFO',
        'type': 'CODE_SMELL',
        },
     'https://docs.microsoft.com/en-us/cpp/error-messages/compiler-warnings/compiler-warning-level-1-c4600' : {
        'severity': 'INFO',
        'type': 'CODE_SMELL',
        },
     'https://docs.microsoft.com/en-us/cpp/error-messages/compiler-warnings/compiler-warning-level-3-c4800' : {
        'severity': 'INFO',
        'type': 'CODE_SMELL',
        },
     'https://docs.microsoft.com/en-us/cpp/code-quality/code-analysis-for-c-cpp-warnings' : {
        'severity': 'CRITICAL',
        'type': 'CODE_SMELL',
        },
     'https://docs.microsoft.com/en-us/cpp/code-quality/code-analysis-for-cpp-corecheck' : {
        'severity': 'INFO',
        'type': 'CODE_SMELL',
        },
}

# special values for rules (overwriting default)
RULE_MAP = {
    # Compiler warnings C4000 - C5999
    'C4020': {'severity':'MAJOR','type':'BUG'},
    'C4034': {'severity':'MAJOR','type':'BUG'},
    'C4056': {'severity':'MAJOR','type':'BUG'},
    'C4062': {'severity':'MAJOR','type':'BUG'},
    'C4130': {'severity':'MAJOR','type':'BUG'},
    'C4133': {'severity':'MAJOR','type':'BUG'},
    'C4138': {'severity':'MAJOR','type':'BUG'},
    'C4172': {'severity':'MAJOR','type':'BUG'},
    'C4243': {'severity':'MAJOR','type':'BUG'},
    'C4245': {'severity':'MAJOR','type':'BUG'},
    'C4291': {'severity':'MAJOR','type':'BUG'},
    'C4293': {'severity':'MAJOR','type':'BUG'},
    'C4295': {'severity':'MAJOR','type':'BUG'},
    'C4296': {'severity':'MAJOR','type':'BUG'},
    'C4309': {'severity':'MAJOR','type':'BUG'},
    'C4313': {'severity':'MAJOR','type':'BUG'},
    'C4317': {'severity':'MAJOR','type':'BUG'},
    'C4333': {'severity':'MAJOR','type':'BUG'},
    'C4339': {'severity':'MAJOR','type':'BUG'},
    'C4340': {'severity':'MAJOR','type':'BUG'},
    'C4341': {'severity':'MAJOR','type':'BUG'},
    'C4355': {'severity':'MAJOR','type':'BUG'},
    'C4356': {'severity':'MAJOR','type':'BUG'},
    'C4358': {'severity':'MAJOR','type':'BUG'},
    'C4359': {'severity':'MAJOR','type':'BUG'},
    'C4368': {'severity':'MAJOR','type':'BUG'},
    'C4405': {'severity':'MAJOR','type':'BUG'},
    'C4407': {'severity':'MAJOR','type':'BUG'},
    'C4422': {'severity':'MAJOR','type':'BUG'},
    'C4426': {'severity':'MAJOR','type':'BUG'},
    'C4473': {'severity':'MAJOR','type':'BUG'},
    'C4474': {'severity':'MAJOR','type':'BUG'},
    'C4477': {'severity':'MAJOR','type':'BUG'},
    'C4478': {'severity':'MAJOR','type':'BUG'},
    'C4526': {'severity':'MAJOR','type':'BUG'},
    'C4539': {'severity':'MAJOR','type':'BUG'},
    'C4541': {'severity':'MAJOR','type':'BUG'},
    'C4715': {'severity':'MAJOR','type':'BUG'},
    'C4716': {'severity':'MAJOR','type':'BUG'},
    'C4717': {'severity':'MAJOR','type':'BUG'},
    'C4756': {'severity':'MAJOR','type':'BUG'},
    'C4774': {'severity':'MINOR','type':'CODE_SMELL'},
    'C4777': {'severity':'MINOR','type':'CODE_SMELL'},
    # Code analysis for C/C++ warnings
    'C6001': {'severity':'CRITICAL','type':'BUG'},
    'C6011': {'severity':'CRITICAL','type':'BUG'},
    'C6014': {'severity':'BLOCKER','type':'BUG'},
    'C6057': {'severity':'CRITICAL','type':'BUG'},
    'C6063': {'severity':'CRITICAL','type':'BUG'},
    'C6064': {'severity':'CRITICAL','type':'BUG'},
    'C6066': {'severity':'CRITICAL','type':'BUG'},
    'C6101': {'severity':'CRITICAL','type':'BUG'},
    'C6102': {'severity':'CRITICAL','type':'BUG'},
    'C6103': {'severity':'CRITICAL','type':'BUG'},
    'C6200': {'severity':'CRITICAL','type':'BUG'},
    'C6201': {'severity':'CRITICAL','type':'BUG'},
    'C6202': {'severity':'CRITICAL','type':'BUG'},
    'C6203': {'severity':'CRITICAL','type':'BUG'},
    'C6235': {'severity':'CRITICAL','type':'BUG'},
    'C6236': {'severity':'CRITICAL','type':'BUG'},
    'C6237': {'severity':'CRITICAL','type':'BUG'},
    'C6239': {'severity':'CRITICAL','type':'BUG'},
    'C6240': {'severity':'CRITICAL','type':'BUG'},
    'C6260': {'severity':'CRITICAL','type':'BUG'},
    'C6272': {'severity':'CRITICAL','type':'BUG'},
    'C6277': {'severity':'CRITICAL','type':'VULNERABILITY'},
    'C6278': {'severity':'CRITICAL','type':'BUG'},
    'C6279': {'severity':'CRITICAL','type':'BUG'},
    'C6283': {'severity':'CRITICAL','type':'BUG'},
    'C6287': {'severity':'CRITICAL','type':'BUG'},
    'C6294': {'severity':'CRITICAL','type':'BUG'},
    'C6295': {'severity':'CRITICAL','type':'BUG'},
    'C6296': {'severity':'CRITICAL','type':'BUG'},
    'C6299': {'severity':'CRITICAL','type':'BUG'},
    'C6308': {'severity':'CRITICAL','type':'BUG'},
    'C6318': {'severity':'CRITICAL','type':'BUG'},
    'C6322': {'severity':'CRITICAL','type':'BUG'},
    'C6334': {'severity':'CRITICAL','type':'BUG'},
    'C6335': {'severity':'CRITICAL','type':'BUG'},
    'C6383': {'severity':'CRITICAL','type':'BUG'},
    'C6535': {'severity':'CRITICAL','type':'BUG'},
    # C++ Core Guidelines checker warnings
    'C26100': {'severity':'CRITICAL','type':'CODE_SMELL'},
    'C26101': {'severity':'CRITICAL','type':'CODE_SMELL'},
    'C26105': {'severity':'CRITICAL','type':'CODE_SMELL'},
    'C26110': {'severity':'CRITICAL','type':'CODE_SMELL'},
    'C26111': {'severity':'CRITICAL','type':'CODE_SMELL'},
    'C26112': {'severity':'CRITICAL','type':'CODE_SMELL'},
    'C26115': {'severity':'CRITICAL','type':'CODE_SMELL'},
    'C26116': {'severity':'CRITICAL','type':'CODE_SMELL'},
    'C26117': {'severity':'CRITICAL','type':'CODE_SMELL'},
    'C26130': {'severity':'CRITICAL','type':'CODE_SMELL'},
    'C26135': {'severity':'CRITICAL','type':'CODE_SMELL'},
    'C26140': {'severity':'CRITICAL','type':'CODE_SMELL'},
    'C26160': {'severity':'CRITICAL','type':'CODE_SMELL'},
    'C26165': {'severity':'CRITICAL','type':'CODE_SMELL'},
    'C26166': {'severity':'CRITICAL','type':'CODE_SMELL'},
    'C26167': {'severity':'CRITICAL','type':'CODE_SMELL'},
    'C26400': {'severity':'CRITICAL','type':'CODE_SMELL'},
    'C26401': {'severity':'BLOCKER','type':'BUG'},
    'C26402': {'severity':'BLOCKER','type':'BUG'},
    'C26403': {'severity':'BLOCKER','type':'BUG'},
    'C26404': {'severity':'BLOCKER','type':'BUG'},
    'C26405': {'severity':'BLOCKER','type':'BUG'},
    'C26406': {'severity':'BLOCKER','type':'CODE_SMELL'},
    'C26407': {'severity':'BLOCKER','type':'CODE_SMELL'},
    'C26408': {'severity':'CRITICAL','type':'CODE_SMELL'},
    'C26409': {'severity':'BLOCKER','type':'BUG'},
    'C26410': {'severity':'CRITICAL','type':'CODE_SMELL'},
    'C26411': {'severity':'BLOCKER','type':'BUG'},
    'C26412': {'severity':'MAJOR','type':'CODE_SMELL'},
    'C26423': {'severity':'MAJOR','type':'CODE_SMELL'},
    'C26424': {'severity':'MAJOR','type':'CODE_SMELL'},
    'C26453': {'severity':'BLOCKER','type':'BUG'},
    'C26454': {'severity':'BLOCKER','type':'BUG'},
    'C26460': {'severity':'MAJOR','type':'CODE_SMELL'},
    'C26461': {'severity':'MAJOR','type':'CODE_SMELL'},
    'C26462': {'severity':'MAJOR','type':'CODE_SMELL'},
    'C26463': {'severity':'MAJOR','type':'CODE_SMELL'},
    'C26464': {'severity':'MAJOR','type':'CODE_SMELL'},
    'C26465': {'severity':'MAJOR','type':'CODE_SMELL'},
    'C26466': {'severity':'MAJOR','type':'CODE_SMELL'},
    'C26470': {'severity':'CRITICAL','type':'BUG'},
    'C26471': {'severity':'CRITICAL','type':'BUG'},
    'C26481': {'severity':'CRITICAL','type':'BUG'},
    'C26482': {'severity':'CRITICAL','type':'CODE_SMELL'},
    'C26483': {'severity':'CRITICAL','type':'BUG'},
    'C26485': {'severity':'CRITICAL','type':'CODE_SMELL'},
    'C26486': {'severity':'CRITICAL','type':'CODE_SMELL'},
    'C26487': {'severity':'CRITICAL','type':'CODE_SMELL'},
    'C26489': {'severity':'BLOCKER','type':'BUG'},
    'C26490': {'severity':'CRITICAL','type':'CODE_SMELL'},
    'C26491': {'severity':'CRITICAL','type':'CODE_SMELL'},
    'C26492': {'severity':'CRITICAL','type':'CODE_SMELL'},
    'C26493': {'severity':'CRITICAL','type':'CODE_SMELL'},
    'C26494': {'severity':'MAJOR','type':'BUG'},
    'C26495': {'severity':'MAJOR','type':'BUG'},
    'C26496': {'severity':'MAJOR','type':'CODE_SMELL'},
    'C26497': {'severity':'MAJOR','type':'CODE_SMELL'},
    'C26498': {'severity':'MAJOR','type':'CODE_SMELL'},
    'C28103': {'severity':'INFO','type':'BUG'},
    'C28105': {'severity':'INFO','type':'BUG'},
    'C28114': {'severity':'BLOCKER','type':'BUG'},
}


et = get_cdata_capable_xml_etree()

def read_page_source(browser, url):
    browser.get(url)
    return browser.page_source


def warning_id(menu_item):
    if not menu_item:
        return None
    if 'warnings' in menu_item:
        return None
    match = re.search('(C[0-9]{4,5})', menu_item)
    if not match:
        return None
    return match.group(1)


def parse_warning_hrefs(page_source, warnings):
    # parse HTML page
    soup = BeautifulSoup(page_source, 'html.parser')

    # read all warnings from menu: Cnnnnn
    for menu_item in  soup.find_all('a', href=True, string=re.compile('C[0-9]{4,5}$')):
        id = warning_id(menu_item.string)
        href = menu_item['href']
        if id and href:
            warnings[id] = {'key': id, 'href': href}

    return warnings


def name(elem, id, default_name):
    text = ''
    if elem:
        for string in elem.strings:
            if string == '\n':
                break
            text += string
        prefix = 'Warning '
        if text.startswith(prefix):
             text = text.replace(prefix, "", 1)
        prefix = 'warning '
        if text.startswith(prefix):
             text = text.replace(prefix, "", 1)
        text = text.strip('.')
        match = re.match('^(C[0-9]+)[ :-](.*)', text)
        if match:
            text = match.group(1) + ': ' + match.group(2).strip()
        else:
            text = id + ': ' + text.strip()
    if not text:
        text = default_name
    return text


def parse_warning_page(page_source, warning):
    # parse HTML page
    soup = BeautifulSoup(page_source, 'html.parser')
    content = soup.find('main')

    # use header, sometimes only message ID
    id = warning['key']
    warning['name'] = name(content.find('h1'), id, id)
    # sometimes better description inside blockquote
    warning['name'] = name(content.select_one('blockquote > p'), id, warning['name'])

    desc = ''
    for p in  content.select('main > p'):
        txt = str(p)
        if 'Compiler Warning ' in warning['name']:
            # compiler messages: first p element is header
            if len(txt) < 200:
                warning['name'] = name(p, id, warning['name'])
            else:
                desc += txt
                break
        else:
            # use only first p block: XML otherwise becomes too large
            desc += txt
            break
    if not desc:
        # repeat header in description to have something
        desc = '<p>'  + EntitySubstitution().substitute_html(warning['name']) + '</p>'
    warning['description'] = desc
    return warning


def read_warning_pages(browser, warnings):
    # read HTML pages of warnings
    for id, data in warnings.items():
        page_source = read_page_source(browser, data['href'])
        data = parse_warning_page(page_source, data)


def description(data):
    html = '\n' + data['description']
    html += '\n<h2>Microsoft Documentation</h2>'
    html += '\n<p><a href="{}" target="_blank">{}</a></p>'.format(data['href'], data['key'])
    return html


def create_template_rules(rules):
    rule_key = 'CustomRuleTemplate'
    rule_name = 'Template for custom Custom rules'
    rule_severity = 'MAJOR' 
    rule_description = """<p>Follow these steps to make your custom Custom rules available in SonarQube:</p>
<ol>
  <ol>
    <li>Create a new rule in SonarQube by "copying" this rule template and specify the <code>CheckId</code> of your custom rule, a title, a description, and a default severity.</li>
    <li>Enable the newly created rule in your quality profile</li>
  </ol>
  <li>Relaunch an analysis on your projects, et voilà, your custom rules are executed!</li>
</ol>"""

    rule = et.Element('rule')
    et.SubElement(rule, 'key').text = rule_key
    et.SubElement(rule, 'cardinality').text = "MULTIPLE"
    name = et.SubElement(rule, 'name').text=rule_name
    et.SubElement(rule, 'description').append(CDATA(rule_description))
    et.SubElement(rule, 'severity').text = rule_severity
    rules.append(rule)


def create_rules(warnings, rules):
    for id, data in warnings.items():
        rule = et.Element('rule')

        # mandatory
        et.SubElement(rule, 'key').text = data['key']
        et.SubElement(rule, 'name').text = data['name']
        cdata = CDATA(description(data))
        et.SubElement(rule, 'description').append(cdata)

        # optional
        if 'tag' in data:
            for tag in data['tag'].split(','):
                et.SubElement(rule, 'tag').text = tag

        if 'internalKey' in data:
            et.SubElement(rule, 'internalKey').text = data['internalKey']

        if 'severity' in data:
            et.SubElement(rule, 'severity').text = data['severity']
        else:
            et.SubElement(rule, 'severity').text = 'INFO'

        if 'type' in data:
            et.SubElement(rule, 'type').text = data['type']
        else:
            et.SubElement(rule, 'type').text = 'CODE_SMELL'

        if ('remediationFunction' in data) and ('remediationFunctionGapMultiplier' in data):
            et.SubElement(rule, 'remediationFunction').text = data['remediationFunction']
            et.SubElement(rule, 'remediationFunctionGapMultiplier').text = data['remediationFunctionGapMultiplier']
        elif ('type' in data) and (data['type'] != 'INFO'):
            et.SubElement(rule, 'remediationFunction').text = 'LINEAR'
            et.SubElement(rule, 'remediationFunctionGapMultiplier').text = '5min'

        rules.append(rule)


def sorter(item):
    return  int(item[0][1:])


def assign_warning_properties(warning, defaults, override):
    for key, value in defaults.items():
        if override:
            warning[key] = value
        elif key not in warning:
                warning[key] = value


def read_warnings():
    # page contains JavaScript. Use Firefox to create HTML page
    # you have to download and install https://github.com/mozilla/geckodriver/releases
    browser = webdriver.Firefox(executable_path=r'C:\Program Files\geckodriver\geckodriver.exe')

    # read links to warning pages from menu of overview pages
    warnings = {}
    for url, properties in URLS.items():
        page_source = read_page_source(browser, url)
        parse_warning_hrefs(page_source, warnings)
        for key, warning in warnings.items():
            assign_warning_properties(warning, properties, False)

    ### warnings = dict(list(warnings.items())[:1]) ##### test
    
    # sort warnings ascending by message number
    warnings = dict(sorted(warnings.items(), key=sorter))

    # read cotent of warning pages
    read_warning_pages(browser, warnings)

    # override defaults
    for key, defaults in RULE_MAP.items():
        if key in warnings:
            warning = warnings[key]
            assign_warning_properties(warning, defaults, True)

    # close browser
    browser.quit()
    return warnings


def create_xml(warnings):
    rules = et.Element('rules')

    create_template_rules(rules)
    create_rules(warnings, rules)

    write_rules_xml(rules, sys.stdout)


if __name__ == "__main__":
    warnings = read_warnings()
    create_xml(warnings)
    sys.exit(0)
