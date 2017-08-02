{{! create PolicyData for DNC }}
{
{{#connections}}
{
{{#terminals}}
{{toJSON .}}
{{~#unless @last}},{{/unless}}
{{/terminals}}
}
{{~#unless @last}},{{/unless}}
{{/connections}}
}
