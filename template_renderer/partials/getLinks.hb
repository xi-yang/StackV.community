{{! create PolicyData for DNC }}
{
{{#connections}}
{{toJSON name}}:{
{{#terminals}}
{{toJSON .}}
{{~#unless @last}},{{/unless}}
{{/terminals}}
}
{{~#unless @last}},{{/unless}}
{{/connections}}
}
