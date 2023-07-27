from flask import Flask, request
import sys


app = Flask(__name__)

from langchain.llms import OpenAI
from langchain import PromptTemplate, LLMChain
from langchain.chat_models import ChatOpenAI


@app.route("/gherkin", methods=['post'])
def generateGherkin():
    # if('openai-api-key' not in request.headers):
    #     return "Error: OpenAI API key"
    # if('openai-organization' not in request.headers):
    #     return "Error: OpenAI Organization"
       # api_key = request.headers['openai-api-key']
    # orga_key = request.headers['openai-organization']

    api_key = sys.argv[1]
    orga_key = sys.argv[2]
    if('inputs' not in request.form):
        return "Error: Inputs are required"
    if('description' not in request.form):
        return "Error: Description is required"
    output = ""
    if('output' in request.form):
        output = request.form['output']
    
    llm = OpenAI(openai_api_key=api_key, openai_organization=orga_key)


    # llm = ChatOpenAI(
    #     model_name='gpt-3.5-turbo-16k',
    #     openai_api_key=api_key)

    f = open("gherkin.prompt", "r")
    template = f.read()

    prompt = PromptTemplate(template=template, input_variables=["description"])
    llm_chain = LLMChain(prompt=prompt, llm=llm)
    prompt_input = {'description':request.form['description']}
    result = llm_chain.run(prompt_input)

    f = open("check_gherkin.prompt", "r")
    template = f.read()

    prompt = PromptTemplate(template=template, input_variables=["inputs","description","output"])  
    llm_chain = LLMChain(prompt=prompt, llm=llm)
    prompt_input = {'output':output, 'inputs':request.form['inputs'], 'description':result}
    result1 = llm_chain.run(prompt_input)
    print([result1,"###",result])
    if("NO" in result1):
        return "INVALID REQUEST, CHECK IF YOUR DATA INPUTS AND DESCRIPTION ARE COHERENT, AND TRY AGAIN"
    # return [result1,"###",result]
    return result

@app.route("/verify_gherkin", methods=['post'])
def verifyGherkin():
    # if('openai-api-key' not in request.headers):
    #     return "Error: OpenAI API key"
    # if('openai-organization' not in request.headers):
    #     return "Error: OpenAI Organization"

    # api_key = request.headers['openai-api-key']
    # orga_key = request.headers['openai-organization']

    api_key = sys.argv[1]
    orga_key = sys.argv[2]
    if('inputs' not in request.form):
        return "Error: Inputs are required"
    if('description' not in request.form):
        return "Error: Description is required"
    
    output = ""
    if('output' in request.form):
        output = request.form['output']
    
    llm = OpenAI(openai_api_key=api_key, openai_organization=orga_key)
    f = open("check_gherkin.prompt", "r")
    template = f.read()

    prompt = PromptTemplate(template=template, input_variables=["inputs","description","output"])
    llm_chain = LLMChain(prompt=prompt, llm=llm)
    prompt_input = {'output':output, 'inputs':request.form['inputs'], 'description':request.form['description']}
    result = llm_chain.run(prompt_input)
    return result


@app.route("/groovy", methods=['post'])
def generateGroovy():
    # if('openai-api-key' not in request.headers):
    #     return "Error: OpenAI API key"
    
    # if('openai-organization' not in request.headers):
    #     return "Error: OpenAI Organization"
    
    if('inputs' not in request.form):
        return "Error: Inputs are required"
    
    if('description' not in request.form):
        return "Error: Description is required"
    
    output = ""
    if('output' in request.form):
        output = request.form['output']

    # api_key = request.headers['openai-api-key']
    # orga_key = request.headers['openai-organization']

    api_key = sys.argv[1]
    orga_key = sys.argv[2]

    llm = OpenAI(openai_api_key=api_key, openai_organization=orga_key)
    f = open("groovy.prompt", "r")
    template = f.read()

    prompt = PromptTemplate(template=template, input_variables=["inputs","description","output"])
    llm_chain = LLMChain(prompt=prompt, llm=llm)
    prompt_input = {'output':output, 'inputs':request.form['inputs'], 'description':request.form['description']}
    result = llm_chain.run(prompt_input)
    return result

app.run(debug=True,host='0.0.0.0',port=3001)