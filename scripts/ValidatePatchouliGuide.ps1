$ErrorActionPreference = 'Stop'
$projectRoot = Split-Path -Parent $PSScriptRoot
$resources = Join-Path $projectRoot 'src/main/resources'
$book = Get-Content -Raw (Join-Path $resources 'data/tutorial_mod/patchouli_books/astral_guide/book.json') | ConvertFrom-Json -AsHashtable
if (!$book.use_resource_pack -or $book.custom_book_item -ne 'tutorial_mod:astral_guide') { throw 'Invalid book configuration' }
foreach ($language in @('en_us', 'zh_cn')) {
    $root = Join-Path $resources "assets/tutorial_mod/patchouli_books/astral_guide/$language"
    $entryRoot = Join-Path $root 'entries'
    $entries = @(Get-ChildItem $entryRoot -Recurse -Filter *.json)
    foreach ($file in $entries) {
        $entry = Get-Content -Raw $file.FullName | ConvertFrom-Json -AsHashtable
        $category = $entry.category.Replace('tutorial_mod:', '')
        if (!(Test-Path (Join-Path $root "categories/$category.json"))) { throw "Missing category: $category" }
        if ($entry.pages.Count -lt 1) { throw "Empty entry: $($file.FullName)" }
        foreach ($page in $entry.pages) {
            foreach ($recipeKey in @('recipe', 'recipe2')) {
                if ($page.ContainsKey($recipeKey)) {
                    $recipe = $page[$recipeKey].Replace('tutorial_mod:', '')
                    if (!(Test-Path (Join-Path $resources "data/tutorial_mod/recipes/$recipe.json"))) { throw "Missing recipe: $recipe" }
                }
            }
            $references = @($page.entries)
            foreach ($match in [regex]::Matches([string]$page.text, '\$\(l:([^\)]+)\)')) { $references += $match.Groups[1].Value }
            foreach ($reference in $references) {
                if ([string]::IsNullOrEmpty($reference)) { continue }
                $target = $reference.Replace('tutorial_mod:', '').Split('#')[0]
                if (!(Test-Path (Join-Path $entryRoot "$target.json"))) { throw "Missing entry link: $reference" }
            }
            if ($page.type -eq 'patchouli:multiblock') {
                foreach ($layer in $page.multiblock.pattern) {
                    foreach ($row in $layer) {
                        foreach ($character in $row.ToCharArray()) {
                            if (!$page.multiblock.mapping.ContainsKey([string]$character)) { throw "Unmapped multiblock character: $character" }
                        }
                    }
                }
            }
        }
    }
    Write-Output "$language`: $($entries.Count) entries, categories, recipes, links and multiblocks validated."
}
