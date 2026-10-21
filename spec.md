# SPEC — Aplicativo Android de Controle de Cultivo de Cogumelos

## 1. Objetivo

Criar um aplicativo Android nativo em **Kotlin** para gerenciamento pessoal de cultivos de cogumelos.

O aplicativo será um diário e sistema de controle para:

* Cultivos
* Culturas
* Linhagens
* Incubadoras
* Observações
* Fotos
* Colheitas
* Materiais
* Estoque
* Lista de compras
* Lembretes
* Anotações
* Backup

O aplicativo deve funcionar **100% localmente e offline**.

Não utilizar servidor, backend, login ou banco de dados remoto.

---

# 2. Stack

Utilizar:

* Kotlin
* Android nativo
* Jetpack Compose
* Material 3
* Room
* ViewModel
* StateFlow / Flow
* Kotlin Coroutines
* Navigation Compose
* WorkManager
* Android Photo Picker
* MVVM

Preferir bibliotecas oficiais Android/Jetpack.

Evitar dependências desnecessárias.

---

# 3. Regra principal: armazenamento local

Todos os dados devem permanecer no dispositivo.

Utilizar:

### Banco

Room para:

* Cultivos
* Culturas
* Linhagens
* Eventos
* Observações
* Colheitas
* Incubadoras
* Materiais
* Movimentações de estoque
* Lista de compras
* Lembretes
* Anotações
* Metadados das fotos

### Fotos

Não armazenar imagens grandes diretamente como BLOB no Room.

Armazenar os arquivos localmente e guardar no Room somente os caminhos/URIs e metadados necessários.

### Internet

O aplicativo não deve depender de internet para nenhuma função principal.

---

# 4. Estrutura principal

A navegação principal deverá possuir:

```text
Dashboard
Cultivos
Linhagens
Incubadora
Materiais
Anotações
Configurações
```

---

# 5. Dashboard

O dashboard deve mostrar rapidamente:

* Cultivos ativos
* Cultivos em incubação
* Culturas ativas
* Cultivos com problemas
* Materiais abaixo do estoque mínimo
* Próximos lembretes
* Atividades recentes

Atalhos:

```text
+ Novo cultivo
+ Nova cultura
+ Nova observação
+ Adicionar material
```

O dashboard deve ser simples.

Não transformar em um painel cheio de gráficos.

---

# 6. Cultivos

Um cultivo representa uma execução específica.

Exemplo:

```text
CT-001
Nome: Cultivo A
Espécie: Espécie X
Linhagem: Linhagem Y
```

Campos:

* ID
* Código
* Nome
* Espécie
* Variedade
* Linhagem
* Data de início
* Status
* Método/substrato
* Origem
* Incubadora
* Observações
* Fotos
* Data de finalização

Status:

```text
Planejado
Preparação
Incubação
Colonização
Frutificação
Colheita
Finalizado
Contaminado
Descartado
```

---

# 7. Timeline do cultivo

Cada cultivo deve possuir uma timeline.

O usuário pode registrar eventos como:

* Início
* Observação
* Foto
* Mudança de etapa
* Transferência
* Problema
* Colheita
* Finalização
* Nota

Cada evento:

```text
Data/hora
Tipo
Descrição
Fotos opcionais
```

Exemplo:

```text
08/10
Cultivo iniciado

12/10
Observação
Desenvolvimento inicial.

17/10
Observação + foto
Desenvolvimento registrado.

25/10
Mudança de etapa

30/10
Colheita
```

A timeline deve ser cronológica e fácil de visualizar.

---

# 8. Linhagem

A linhagem é um dos conceitos centrais do aplicativo.

O sistema deve permitir rastrear a origem e os derivados de uma cultura.

Exemplo:

```text
LINHAGEM X
│
├── Ágar AG-001
│   └── Cultura líquida CL-001
│       ├── Spawn SP-001
│       │   └── Cultivo CT-001
│       │
│       └── Spawn SP-002
│           └── Cultivo CT-002
│
└── Ágar AG-002
    └── Cultura líquida CL-002
        └── Cultivo CT-003
```

Cada registro relacionado pode possuir:

* Registro pai
* Registros derivados

O aplicativo deve conseguir mostrar visualmente essa árvore.

A estrutura precisa ser genérica.

Não criar lógica específica para uma única espécie.

---

# 9. Culturas

Criar uma área para controle das culturas mantidas.

Tipos:

```text
Cultura líquida
Ágar
Spawn
Matriz
Outro
```

Campos:

* ID
* Código
* Nome
* Espécie
* Variedade
* Linhagem
* Tipo
* Data de criação
* Origem
* Registro pai
* Local
* Incubadora
* Status
* Observações
* Fotos

Status:

```text
Ativo
Em observação
Suspeita de contaminação
Contaminado
Descartado
Arquivado
```

---

# 10. Incubadora

Criar uma área para registrar incubadoras/localizações.

Campos:

* Nome
* Código
* Local
* Descrição
* Temperatura registrada manualmente
* Umidade registrada manualmente
* Observações

Permitir associar:

* Culturas
* Cultivos

a uma incubadora.

Não implementar sensores nesta primeira versão.

---

# 11. Observações

Criar um sistema rápido de observações.

Campos:

* Data/hora
* Texto
* Registro relacionado
* Temperatura opcional
* Umidade opcional
* Etapa
* Fotos

O usuário deve conseguir registrar uma observação rapidamente.

Não obrigar preenchimento de campos que não sejam necessários.

---

# 12. Colheitas

Um cultivo pode possuir várias colheitas.

Campos:

* Número da colheita
* Data
* Quantidade
* Unidade
* Observação
* Fotos

Mostrar:

```text
Colheita 1 — 120 g
Colheita 2 — 180 g
Colheita 3 — 90 g

Total: 390 g
```

---

# 13. Materiais / Estoque

Criar uma área específica para controlar os materiais disponíveis.

Exemplos:

```text
Vermiculita
Álcool
Luvas
Substratos
Sacos
Placas
Recipientes
Equipamentos
Ferramentas
Outros
```

Cada material deve possuir:

* Nome
* Categoria
* Quantidade atual
* Unidade
* Estoque mínimo
* Estoque ideal
* Local
* Observações
* Data da última compra
* Preço opcional

Categorias:

```text
Substrato
Higiene
Consumível
Equipamento
Recipiente
Ferramenta
Outro
```

---

# 14. Movimentação de estoque

Registrar entradas e saídas.

Exemplo:

```text
VERMICULITA

Estoque atual: 1,2 kg

28/09  +5 kg
Compra

02/10  -300 g
Cultivo CT-003

08/10  -500 g
Cultivo CT-004
```

Cada movimentação deve conter:

* Data/hora
* Tipo
* Quantidade
* Motivo
* Cultivo relacionado opcional
* Observação

Tipos:

```text
Entrada
Saída
Ajuste
```

Não permitir estoque negativo sem confirmação explícita do usuário.

---

# 15. Estoque mínimo

Cada material possui:

```text
Quantidade atual
Estoque mínimo
Estoque ideal
```

Exemplo:

```text
Vermiculita

Atual: 300 g
Mínimo: 500 g
Ideal: 2 kg
```

Quando o estoque estiver abaixo do mínimo:

* Mostrar alerta no dashboard
* Marcar material como baixo estoque
* Adicionar à lista de compras

---

# 16. Lista de compras

Criar uma lista de compras.

Pode ser preenchida automaticamente quando materiais atingirem o estoque mínimo.

Exemplo:

```text
LISTA DE COMPRAS

☐ Vermiculita
☐ Luvas
☐ Álcool
```

Permitir:

* Adicionar manualmente
* Remover
* Marcar como comprado
* Registrar reposição

Ao registrar uma compra, atualizar o estoque.

---

# 17. Lembretes

Permitir criar lembretes locais.

Exemplo:

```text
Álcool
Verificar estoque
A cada 30 dias
```

Outro:

```text
Luvas
Verificar estoque
A cada 15 dias
```

O usuário deve conseguir configurar:

* Ativado/desativado
* Data
* Horário
* Intervalo
* Repetição
* Descrição

Utilizar WorkManager/notificações locais.

Os lembretes são independentes do alerta de estoque mínimo.

---

# 18. Anotações

Criar uma área de anotações gerais.

Uma anotação pode ser:

* Livre
* Ligada a cultivo
* Ligada a cultura
* Ligada a linhagem
* Ligada a material

Campos:

* Título
* Texto
* Data
* Tags
* Fotos
* Registro relacionado

Adicionar busca por texto.

---

# 19. Fotos

Fotos devem ser tratadas como parte importante do aplicativo.

Permitir:

* Tirar foto
* Selecionar fotos
* Múltiplas fotos
* Legenda
* Data
* Galeria
* Exclusão

Fotos podem pertencer a:

```text
Cultivo
Cultura
Linhagem
Observação
Evento
Colheita
Material
Anotação
```

As imagens devem ficar armazenadas localmente.

Evitar deixar arquivos órfãos após exclusões.

---

# 20. Busca

Implementar busca local.

Pesquisar:

* Cultivos
* Culturas
* Linhagens
* Materiais
* Anotações

Filtros:

* Status
* Espécie
* Tipo
* Categoria
* Data

Não criar mecanismo de busca complexo na primeira versão.

---

# 21. Banco de dados

Criar entidades Room apropriadas.

Sugestão:

```text
Cultivation
Culture
Lineage
CultivationEvent
Observation
Harvest
Incubator
Material
StockMovement
ShoppingItem
Reminder
Note
Photo
```

A modelagem pode ser modificada se houver uma solução melhor, mas deve preservar os conceitos.

Utilizar Foreign Keys quando apropriado.

Utilizar migrations do Room.

Não apagar dados silenciosamente.

---

# 22. Backup

Criar sistema de backup local.

Funções:

```text
Exportar backup
Importar backup
Último backup
Restaurar
```

O backup deve incluir:

* Dados
* Fotos
* Metadados

Preferir arquivo ZIP.

Estrutura sugerida:

```text
backup/
├── manifest.json
├── database.json
└── photos/
```

O `manifest.json` deve possuir versão do formato.

Exemplo:

```json
{
  "backupVersion": 1,
  "createdAt": "...",
  "appVersion": "..."
}
```

Antes de restaurar:

* Validar o arquivo
* Verificar versão
* Confirmar com o usuário
* Evitar sobrescrever dados sem confirmação

---

# 23. Interface

Visual:

* Dark como tema principal
* Limpo
* Rústico/discreto
* Funcional
* Boa leitura
* Ícones simples
* Fotos valorizadas

Não utilizar visual excessivamente corporativo.

Não exagerar nas animações.

Suportar:

```text
Tema escuro
Tema claro
Seguir sistema
```

Usar Material 3.

---

# 24. Estrutura de navegação

```text
Dashboard
│
├── Cultivos
│   ├── Lista
│   ├── Novo
│   └── Detalhes
│       ├── Timeline
│       ├── Fotos
│       ├── Observações
│       └── Colheitas
│
├── Linhagens
│   ├── Lista
│   ├── Nova
│   └── Árvore
│
├── Incubadora
│   ├── Lista
│   └── Detalhes
│
├── Materiais
│   ├── Estoque
│   ├── Lista de compras
│   ├── Movimentações
│   └── Detalhes
│
├── Anotações
│   ├── Lista
│   └── Detalhes
│
└── Configurações
    ├── Backup
    ├── Notificações
    └── Aparência
```

---

# 25. Qualidade

O aplicativo deve:

* Validar entradas
* Evitar dados inconsistentes
* Não perder dados
* Funcionar após fechar e abrir
* Funcionar offline
* Evitar crashes
* Solicitar somente permissões necessárias
* Manter estado corretamente
* Tratar erros de armazenamento
* Tratar falhas de backup/restauração

---

# 26. Testes

Criar testes para as partes críticas.

Prioridade:

1. Room
2. Cultivos
3. Linhagem
4. Estoque
5. Estoque mínimo
6. Lista de compras
7. Lembretes
8. Backup
9. Fotos

Testar especialmente:

* Criar cultivo
* Editar cultivo
* Criar linhagem
* Criar relação pai/filho
* Registrar entrada
* Registrar saída
* Detectar estoque baixo
* Gerar item de compra
* Restaurar backup
* Manter integridade das fotos

---

# 27. Ordem de implementação

Não implementar tudo de uma vez.

## Fase 1 — Fundação

Implementar:

* Projeto Android
* Kotlin
* Compose
* Material 3
* Room
* Navigation
* ViewModel
* Tema
* Dashboard básico

Garantir que compile e execute.

---

## Fase 2 — Cultivos

Implementar:

* CRUD
* Lista
* Detalhes
* Status
* Timeline
* Observações
* Fotos

Testar completamente antes de avançar.

---

## Fase 3 — Linhagens e culturas

Implementar:

* Culturas
* Ágar
* Cultura líquida
* Spawn
* Matriz
* Relações pai/filho
* Árvore de linhagem

---

## Fase 4 — Incubadora

Implementar:

* Incubadoras
* Associação de cultivos
* Associação de culturas
* Registros ambientais manuais

---

## Fase 5 — Estoque

Implementar:

* Materiais
* Entradas
* Saídas
* Ajustes
* Estoque mínimo
* Estoque ideal
* Histórico
* Lista de compras

---

## Fase 6 — Notificações

Implementar:

* Lembretes
* Alertas de estoque
* WorkManager
* Notificações locais

---

## Fase 7 — Anotações e busca

Implementar:

* Anotações
* Tags
* Busca
* Filtros

---

## Fase 8 — Backup

Implementar:

* Exportação
* Importação
* Fotos
* Versionamento
* Validação

---

## Fase 9 — Refinamento

Revisar:

* UX
* Performance
* Acessibilidade
* Estados vazios
* Mensagens de erro
* Loading
* Testes
* Bugs
* Integridade dos dados

---

# 28. Regras para o Codex

Antes de implementar:

1. Analise o projeto atual.
2. Se estiver vazio, crie corretamente a estrutura Android.
3. Não crie backend.
4. Não adicione Firebase.
5. Não adicione login.
6. Não adicione serviços pagos.
7. Não dependa de internet.
8. Prefira bibliotecas oficiais Android/Jetpack.
9. Mantenha a arquitetura simples.
10. Não crie funcionalidades não especificadas sem necessidade.
11. Compile e teste após cada fase.
12. Corrija erros antes de avançar.
13. Não apenas descreva a implementação: implemente.
14. Use Room para persistência.
15. Use migrations.
16. Não mantenha dados importantes somente em memória.
17. Preserve integridade das relações.
18. Não apague dados silenciosamente.
19. Faça commits lógicos quando possível.
20. Mantenha o aplicativo funcional após cada etapa.

---

# 29. Resultado final esperado

O aplicativo deve permitir acompanhar o ciclo completo:

```text
LINHAGEM
    ↓
CULTURA
    ↓
DERIVAÇÕES
    ↓
CULTIVO
    ↓
OBSERVAÇÕES
    ↓
FRUTIFICAÇÃO
    ↓
COLHEITAS
```

E simultaneamente:

```text
MATERIAIS
    ↓
ESTOQUE
    ↓
CONSUMO
    ↓
ESTOQUE MÍNIMO
    ↓
LISTA DE COMPRAS
    ↓
LEMBRETES
```

Tudo deve ser:

* Local
* Offline
* Privado
* Rastreável
* Com fotos
* Com histórico
* Com backup
* Com notificações locais

---

# 30. Critério principal

O aplicativo não deve parecer um CRUD genérico.

Ele deve parecer uma ferramenta pessoal de acompanhamento de cultivo, com foco em:

**confiabilidade > simplicidade > velocidade > quantidade de funcionalidades.**

Comece pela **Fase 1**.

Não implemente as fases seguintes até que a fase atual esteja compilando e funcionando corretamente.
