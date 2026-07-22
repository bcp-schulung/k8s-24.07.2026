{{/*
Expand the name of the chart.
*/}}
{{- define "joke-platform.name" -}}
{{- default .Chart.Name .Values.nameOverride | trunc 63 | trimSuffix "-" }}
{{- end }}

{{/*
Create a default fully qualified app name.
*/}}
{{- define "joke-platform.fullname" -}}
{{- if .Values.fullnameOverride }}
{{- .Values.fullnameOverride | trunc 63 | trimSuffix "-" }}
{{- else }}
{{- $name := default .Chart.Name .Values.nameOverride }}
{{- if contains $name .Release.Name }}
{{- .Release.Name | trunc 63 | trimSuffix "-" }}
{{- else }}
{{- printf "%s-%s" .Release.Name $name | trunc 63 | trimSuffix "-" }}
{{- end }}
{{- end }}
{{- end }}

{{/*
Create chart name and version as used by the chart label.
*/}}
{{- define "joke-platform.chart" -}}
{{- printf "%s-%s" .Chart.Name .Chart.Version | replace "+" "_" | trunc 63 | trimSuffix "-" }}
{{- end }}

{{/*
Common labels
*/}}
{{- define "joke-platform.labels" -}}
helm.sh/chart: {{ include "joke-platform.chart" . }}
{{ include "joke-platform.selectorLabels" . }}
{{- if .Chart.AppVersion }}
app.kubernetes.io/version: {{ .Chart.AppVersion | quote }}
{{- end }}
app.kubernetes.io/managed-by: {{ .Release.Service }}
app.kubernetes.io/part-of: joke-platform
{{- end }}

{{/*
Selector labels
*/}}
{{- define "joke-platform.selectorLabels" -}}
app.kubernetes.io/name: {{ include "joke-platform.name" . }}
app.kubernetes.io/instance: {{ .Release.Name }}
{{- end }}

{{/*
Service labels
*/}}
{{- define "joke-platform.serviceLabels" -}}
{{- $service := index . 0 -}}
{{- $context := index . 1 -}}
app.kubernetes.io/name: {{ $service.name }}
app.kubernetes.io/component: {{ $service.component | default "backend" }}
app.kubernetes.io/part-of: joke-platform
helm.sh/chart: {{ include "joke-platform.chart" $context }}
app.kubernetes.io/managed-by: {{ $context.Release.Service }}
{{- end }}

{{/*
Service selector labels
*/}}
{{- define "joke-platform.serviceSelectorLabels" -}}
{{- $service := index . 0 -}}
app.kubernetes.io/name: {{ $service.name }}
app.kubernetes.io/component: {{ $service.component | default "backend" }}
{{- end }}

{{/*
Create the image path
*/}}
{{- define "joke-platform.image" -}}
{{- $service := index . 0 -}}
{{- $context := index . 1 -}}
{{- printf "%s/%s:%s" $context.Values.global.imageRegistry $service.image.repository $service.image.tag }}
{{- end }}
